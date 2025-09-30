package com.example.prototypeapp.data.model.estimator;

import com.example.prototypeapp.domain.locationEstimator.LocationEstimator;

public class LocationEstimate {
    public  double x;
    public  double y;
    public  double accuracy;  // accuracy for weights in combined estimator
    public  String source;    // type of location estimator
    public long timeStamp = 0;
    public int effectiveBeacons = 0;
    public double rms = 0;
    public  boolean valid;

    public LocationEstimate(double x, double y, double accuracy, String source, boolean valid){
        this.x = x;
        this.y = y;
        this.accuracy = accuracy;
        this.source = source;
        this.valid = valid;
    }

    public LocationEstimate(double x, double y, double accuracy, String source, long timeStamp, int effectiveBeacons, double rms, boolean valid) {
        this.x = x;
        this.y = y;
        this.accuracy = accuracy;
        this.source = source;
        this.timeStamp = timeStamp;
        this.effectiveBeacons = effectiveBeacons;
        this.rms = rms;
        this.valid = valid;
    }

    public static LocationEstimate invalid(String method) {
        return new LocationEstimate(0,0, Double.MAX_VALUE, method, false);
    }
}
