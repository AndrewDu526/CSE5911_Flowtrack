package com.example.prototypeapp.domain.locationFilter;

import com.example.prototypeapp.data.model.TrackBatch;
import com.example.prototypeapp.data.model.TrackPoint;

import org.apache.commons.math3.filter.KalmanFilter;
import org.apache.commons.math3.filter.DefaultProcessModel;
import org.apache.commons.math3.filter.DefaultMeasurementModel;
import org.apache.commons.math3.filter.MeasurementModel;
import org.apache.commons.math3.filter.ProcessModel;
import org.apache.commons.math3.linear.*;



/**
 * 2D 匀速 Kalman 滤波（Apache Commons Math）
 * - 量测：WLS 输出的 (x,y)
 * - 支持：变 dt、自适应 R（按锚点数/残差缩放）、异常门控（马氏距离）
 */
public class KalmanAdaptiveFilter {

    private double measStdBase = 2;     // 基础量测标准差（米） -> R_base = diag(σ^2, σ^2)
    private double accelStd    = 2;     // 过程噪声：加速度标准差（m/s^2）
    private double initPosStd  = 3.0;     // 初始位置标准差（米）
    private double initVelStd  = 1.0;     // 初始速度标准差（m/s）
    private double gateChi2    = 9.21;    // 门控阈值（马氏距离平方），>阈值则拒绝更新
    private double residualRef = 10;     // 残差参考值（米），用于自适应 R 的缩放

    private KalmanFilter kf;              // Apache Commons Math 的滤波器实例
    private RealVector xState;            // 当前状态向量 [x,y,vx,vy]^T
    private RealMatrix Pcov;              // 当前协方差矩阵 4x4
    private long lastTsMs = -1;           // 上一帧时间戳（毫秒）

    // Constant matrix
    private static final RealMatrix H = new Array2DRowRealMatrix(new double[][]{
            {1, 0, 0, 0},
            {0, 1, 0, 0}
    });

    /**
     * initialization
     * @param x0       first frame x (m)
     * @param y0       first frame y (m)
     * @param tsMs     first frame time stamp (ms)
     */
    public void init(double x0, double y0, long tsMs) {
        this.xState = new ArrayRealVector(new double[]{x0, y0, 0, 0}); // initial state
        this.Pcov   = diag(new double[]{sq(initPosStd), sq(initPosStd), sq(initVelStd), sq(initVelStd)}); //
        this.lastTsMs = tsMs;

        double dt = 1.0;
        RealMatrix A = transition(dt);
        RealMatrix Q = processNoise(dt, accelStd);
        RealMatrix R = measurementNoise(sq(measStdBase));

        ProcessModel pm = new DefaultProcessModel(A, zeroB(), Q, xState, Pcov);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);
        kf = new KalmanFilter(pm, mm);

        kf.correct(new double[]{x0, y0});
        pullFromKF();
    }

    /**
     * handle one frame measurement(WLS)
     * @param measX              x
     * @param measY              y
     * @param timestampMs        time stamp
     * @param effectiveAnchors   effective beacons
     * @param wlsRmsResidual     RMS (unknown<=0)
     * @return result
     */
    public TrackPoint step(double measX, double measY, long timestampMs, int effectiveAnchors, double wlsRmsResidual) {
        if (kf == null) {
            init(measX, measY, timestampMs);
            return new TrackPoint(timestampMs, measX, measY, true, effectiveAnchors, 0, wlsRmsResidual, 0);
        }

        // 1) calculate dt
        double dt = Math.max(1e-3, (timestampMs - lastTsMs) / 1000.0);
        lastTsMs = timestampMs;

        // 2) rebuild A/Q and R
        RealMatrix A = transition(dt);
        RealMatrix Q = processNoise(dt, accelStd);
        double measVar = adaptRVar(effectiveAnchors, wlsRmsResidual);
        RealMatrix R = measurementNoise(measVar);

        // 3) rebuild filter with current x/p
        ProcessModel pm = new DefaultProcessModel(A, zeroB(), Q, xState, Pcov);
        MeasurementModel mm = new DefaultMeasurementModel(H, R);
        kf = new KalmanFilter(pm, mm);


        // 4) predict
        kf.predict();

        // 5) gate control
        RealVector xPred = new ArrayRealVector(kf.getStateEstimation());           // double[] -> RealVector
        RealMatrix PPred = new Array2DRowRealMatrix(kf.getErrorCovariance());      // double[][] -> RealMatrix
        RealVector z     = new ArrayRealVector(new double[]{measX, measY});
        RealVector innov = z.subtract(H.operate(xPred));                           // y = z - Hx
        RealMatrix S     = H.multiply(PPred).multiply(H.transpose()).add(R);       // S = HPH^T + R

        boolean accepted = true;
        double m2 = maha2(innov, S);
        if (Double.isFinite(m2) && m2 <= gateChi2) {
            kf.correct(new double[]{measX, measY});
        } else {
            accepted = false;
        }

        // 6) get state and cor
        pullFromKF();

        // 7) get speed
        double[] s = kf.getStateEstimation(); // [x, y, vx, vy]
        double speed = Math.hypot(s[2], s[3]); // m/s

        return new TrackPoint(timestampMs, measX, measY, accepted, effectiveAnchors, speed, wlsRmsResidual, dt);
    }

    // ================== 工具 & 自适应 R ==================

    /** A(dt)：2D 匀速运动的状态转移矩阵 */
    private static RealMatrix transition(double dt) {
        return new Array2DRowRealMatrix(new double[][]{
                {1, 0, dt, 0},
                {0, 1, 0, dt},
                {0, 0, 1 , 0},
                {0, 0, 0 , 1}
        });
    }

    /** Q(dt, accelStd)：过程噪声（由加速度标准差离散化） */
    private static RealMatrix processNoise(double dt, double accelStd) {
        double a2 = accelStd * accelStd;
        double dt2 = dt*dt, dt3 = dt2*dt, dt4 = dt3*dt;
        double q11 = dt4*a2/4.0, q13 = dt3*a2/2.0;
        double q22 = q11,       q24 = q13;
        double q33 = dt2*a2,    q44 = q33;
        return new Array2DRowRealMatrix(new double[][]{
                {q11, 0  , q13, 0  },
                {0  , q22, 0  , q24},
                {q13, 0  , q33, 0  },
                {0  , q24, 0  , q44}
        });
    }

    /** R：量测噪声（x、y 同方差） */
    private static RealMatrix measurementNoise(double var) {
        return new Array2DRowRealMatrix(new double[][]{
                {var, 0},
                {0  , var}
        });
    }

    /** B：无控制量 */
    private static RealMatrix zeroB() {
        return new Array2DRowRealMatrix(new double[][]{{0},{0},{0},{0}});
    }

    /** 自适应量测方差：锚点数 & 残差不好 → R 变大（更不信任量测） */
    private double adaptRVar(int anchors, double residual) {
        double fA; // 锚点数因子
        if (anchors >= 5)      fA = 0.8;
        else if (anchors == 4) fA = 1.0;
        else if (anchors == 3) fA = 1.6;
        else if (anchors == 2) fA = 3.0;
        else                   fA = 6.0;

        double fR; // 残差因子
        if (residual <= 0 || Double.isNaN(residual)) {
            fR = 1.0;
        } else {
            fR = clamp(residual / residualRef, 0.5, 5.0);
        }
        return sq(measStdBase) * fA * fR;
    }

    /** 马氏距离平方 y^T S^{-1} y */
    private static double maha2(RealVector y, RealMatrix S) {
        try {
            DecompositionSolver solver = new LUDecomposition(S).getSolver();
            RealVector SinvY = solver.solve(y);
            return y.dotProduct(SinvY);
        } catch (Exception e) {
            return Double.POSITIVE_INFINITY;
        }
    }

    private void pullFromKF() {
        double[] xs = kf.getStateEstimation();
        this.xState = new ArrayRealVector(xs);
        this.Pcov   = new Array2DRowRealMatrix(kf.getErrorCovariance());
    }


    private static RealMatrix diag(double[] d) { return MatrixUtils.createRealDiagonalMatrix(d); }
    private static double sq(double v){ return v*v; }
    private static double clamp(double v, double lo, double hi){ return Math.max(lo, Math.min(hi, v)); }
}
