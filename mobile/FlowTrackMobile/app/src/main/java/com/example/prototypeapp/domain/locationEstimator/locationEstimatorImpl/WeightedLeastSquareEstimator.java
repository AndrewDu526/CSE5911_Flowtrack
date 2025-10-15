package com.example.prototypeapp.domain.locationEstimator.locationEstimatorImpl;

import com.example.prototypeapp.data.model.beacon.BeaconRuntime;
import com.example.prototypeapp.data.model.beacon.StaticBeacon;
import com.example.prototypeapp.data.model.estimator.LocationEstimate;
import com.example.prototypeapp.domain.locationEstimator.LocationEstimator;
import com.example.prototypeapp.domain.locationEstimator.weightFunction.WeightFunction;
import com.example.prototypeapp.domain.locationEstimator.weightFunction.weightFunctionImpl.ClippedInverseDistanceWeight;

import java.util.List;

public class WeightedLeastSquareEstimator implements LocationEstimator {

    LocationEstimator estimator = new WeightedCentroidEstimator();
    LocationEstimate initialLocationEstimate;
    WeightFunction weightFunction = new ClippedInverseDistanceWeight();

    public  double computeWlsRmsResidual(double xhat, double yhat, List<BeaconRuntime> beacons) {

        if (beacons == null || beacons.isEmpty()) return -1;

        double num = 0.0;
        double den = 0.0;
        for (BeaconRuntime b : beacons) {
            StaticBeacon sb = b.beacon;
            double dx = xhat - sb.x;
            double dy = yhat - sb.y;
            double dHat = Math.hypot(dx, dy);
            double ri   = b.distance;
            double ei   = dHat - ri;

            double wi = Math.max(1e-9, weightFunction.calculate(ri, b.rssi));
            num += wi * ei * ei;
            den += wi;
        }
        return (den > 0) ? Math.sqrt(num / den) : -1;
    }

    @Override
    public LocationEstimate estimate(List<BeaconRuntime> beacons) {

        if (beacons == null || beacons.size() < 3) {
            return LocationEstimate.invalid("WLS: no input");
        }

        double x = 0, y = 0;

        initialLocationEstimate = estimator.estimate(beacons);

        if(!initialLocationEstimate.valid){return LocationEstimate.invalid("WLS: initial locate fail");}

        x = initialLocationEstimate.x;
        y = initialLocationEstimate.y;


        for (int iter = 0; iter < 5; iter++) {
            double Axx = 0, Axy = 0, Ayy = 0;
            double bx  = 0, by  = 0;

            for (BeaconRuntime br : beacons) {
                StaticBeacon sb = br.beacon;

                double dx = x - sb.x;
                double dy = y - sb.y;
                double d  = Math.hypot(dx, dy);
                if (d < 1e-6) d = 1e-6;

                double ri  = br.distance;
                double err = d - ri;

                double gx = dx / d;
                double gy = dy / d;

                double w  = Math.max(1e-9, weightFunction.calculate(ri, br.rssi));

                // A += w * g g^T
                Axx += w * gx * gx;
                Axy += w * gx * gy;
                Ayy += w * gy * gy;

                // b += w * g * err
                bx  += w * gx * err;
                by  += w * gy * err;
            }

            // 解 2x2：A * delta = b
            double det = Axx * Ayy - Axy * Axy;
            if (Math.abs(det) < 1e-9) break;

            double invAxx =  Ayy / det;
            double invAxy = -Axy / det;
            double invAyy =  Axx / det;

            double dxStep = invAxx * bx + invAxy * by;
            double dyStep = invAxy * bx + invAyy * by;

            x -= dxStep;
            y -= dyStep;

            if (dxStep*dxStep + dyStep*dyStep < 1e-6) break; // 提前收敛
        }

        double errSum = 0;
        for (BeaconRuntime beacon : beacons) {
            StaticBeacon sb = beacon.beacon;
            double dx = x - sb.x;
            double dy = y - sb.y;
            double d = Math.sqrt(dx * dx + dy * dy);
            double estError = d - beacon.distance;
            errSum += estError * estError;
        }

        double acc = Math.sqrt(errSum / beacons.size());
        double wrms = computeWlsRmsResidual(x, y, beacons);
        int size = beacons.size();
        long time = System.currentTimeMillis();

        return new LocationEstimate(x, y, acc, "WLS", time, size, wrms, true);

    }
}