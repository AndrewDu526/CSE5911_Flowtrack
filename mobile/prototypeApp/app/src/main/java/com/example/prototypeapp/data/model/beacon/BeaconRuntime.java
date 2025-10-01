package com.example.prototypeapp.data.model.beacon;

import com.example.prototypeapp.domain.rssiSmoother.RssiSmoother;

public class BeaconRuntime {
    public StaticBeacon beacon;
    public double rssi;
    public double distance;
    public RssiSmoother smoother;
    public long lastSeenMs = 0;
    public int  counter = 0;

    public BeaconRuntime(StaticBeacon beacon, double rssi, double distance, RssiSmoother smoother) {
        this.beacon = beacon;
        this.rssi = rssi;
        this.distance = distance;
        this.smoother = smoother;
    }
}
