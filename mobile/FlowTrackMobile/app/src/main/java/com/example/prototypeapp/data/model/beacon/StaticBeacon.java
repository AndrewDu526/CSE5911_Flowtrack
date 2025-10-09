package com.example.prototypeapp.data.model.beacon;

public class StaticBeacon {

    public BeaconId id;
    String name;
    public double x;
    public double y;


    public StaticBeacon(BeaconId id, String name, double x, double y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
    }
}
