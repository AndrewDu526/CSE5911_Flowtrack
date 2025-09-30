package com.example.prototypeapp.data.model;

import android.os.SystemClock;

import com.example.prototypeapp.data.model.estimator.LocationEstimate;

// model to store single location point
public class TrackPoint {


    public final long   time;

    public final double x;
    public final double y;
    public final boolean accepted;
    public final int    anchorsUsed;
    public final double rms;
    public final double dt;

    public TrackPoint(
            long time, double x, double y, boolean accepted, int anchorsUsed, double rms, double dt) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.accepted = accepted;
        this.anchorsUsed = anchorsUsed;
        this.rms = rms;
        this.dt = dt;
    }

}

