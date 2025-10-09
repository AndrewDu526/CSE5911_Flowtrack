package com.example.prototypeapp.domain.locationEstimator.locationEstimatorImpl;

import com.example.prototypeapp.data.model.beacon.BeaconRuntime;
import com.example.prototypeapp.data.model.beacon.StaticBeacon;
import com.example.prototypeapp.data.model.estimator.LocationEstimate;
import com.example.prototypeapp.domain.locationEstimator.LocationEstimator;
import com.example.prototypeapp.domain.locationEstimator.weightFunction.WeightFunction;
import com.example.prototypeapp.domain.locationEstimator.weightFunction.weightFunctionImpl.ClippedInverseDistanceWeight;

import java.util.List;

/** Plain centroid of anchor coordinates (ignores distances) — fast baseline. */
public class WeightedCentroidEstimator implements LocationEstimator {

    WeightFunction weightFunction = new ClippedInverseDistanceWeight();
    double weightedDistance = 0.0;

    @Override
    public LocationEstimate estimate(List<BeaconRuntime> beacons) {
        if (beacons == null || beacons.isEmpty()) {
            return LocationEstimate.invalid("WC: no input");
        }
        double wx = 0, wy = 0, wsum = 0;
        for (BeaconRuntime beacon : beacons) {

            StaticBeacon staticBeacon = beacon.beacon;
            double distance = Math.max(beacon.distance, 1e-6);
            double rssi = beacon.rssi;

            double w = weightFunction.calculate(distance, rssi);

            wx += w * staticBeacon.x;
            wy += w * staticBeacon.y;
            wsum += w;

            weightedDistance += w * distance;
        }
        if (wsum == 0) return LocationEstimate.invalid("WC: zero weight");

        double x = wx / wsum;
        double y = wy / wsum;
        double acc = weightedDistance / wsum;

        return new LocationEstimate(x, y, acc, "WeightedCentroid", true);
    }
}