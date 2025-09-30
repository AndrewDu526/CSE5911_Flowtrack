package com.example.prototypeapp.domain.locationEstimator.weightFunction.weightFunctionImpl;

import com.example.prototypeapp.domain.locationEstimator.weightFunction.WeightFunction;

/**
 * Clipped inverse distance weight: w = A / (d^p + B)
 */
public class ClippedInverseDistanceWeight implements WeightFunction {

    private final double p;
    private final double A;
    private final double B;

    public ClippedInverseDistanceWeight() {
        this(2.0, 1.0, 1.0); // default
    }

    public ClippedInverseDistanceWeight(double p, double A, double B) {
        this.p = p;
        this.A = A;
        this.B = B;
    }

    @Override
    public double calculate(double distance, double rssi) {
        return A / (Math.pow(distance, p) + B);
    }
}
