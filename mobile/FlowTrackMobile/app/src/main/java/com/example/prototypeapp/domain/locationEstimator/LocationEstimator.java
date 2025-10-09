package com.example.prototypeapp.domain.locationEstimator;

import com.example.prototypeapp.data.model.beacon.BeaconRuntime;
import com.example.prototypeapp.data.model.estimator.LocationEstimate;

import java.util.List;

public interface LocationEstimator {
    LocationEstimate estimate(List<BeaconRuntime> beacons);
}
