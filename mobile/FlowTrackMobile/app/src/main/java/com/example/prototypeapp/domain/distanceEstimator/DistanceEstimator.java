package com.example.prototypeapp.domain.distanceEstimator;

public interface DistanceEstimator {
    double estimateDistance(double smoothedRssi);
}
