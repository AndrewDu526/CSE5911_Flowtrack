package com.example.prototypeapp.domain.distanceEstimator.distanceEstimatorImpl;

import com.example.prototypeapp.domain.distanceEstimator.DistanceEstimator;

/**
 * Log-distance path loss model:
 *
 *     d = 10^((TxPower - RSSI) / (10 * n))
 *
 * where:
 *     - TxPower: measured RSSI at 1 meter (e.g. -59 dBm)
 *     - n: path loss exponent (usually 2.0 in open space)
 */
public class LogDistanceEstimator implements DistanceEstimator {

    private final double txPower = -59;
    private final double pathLossExponent = 2.0;

    @Override
    public double estimateDistance(double smoothedRssi) {
        return Math.pow(10.0, (txPower - smoothedRssi) / (10.0 * pathLossExponent));
    }
}
