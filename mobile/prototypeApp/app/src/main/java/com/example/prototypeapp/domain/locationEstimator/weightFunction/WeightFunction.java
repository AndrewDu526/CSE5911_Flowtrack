package com.example.prototypeapp.domain.locationEstimator.weightFunction;

public interface WeightFunction {

    double calculate(double distance, double rssi);
}
