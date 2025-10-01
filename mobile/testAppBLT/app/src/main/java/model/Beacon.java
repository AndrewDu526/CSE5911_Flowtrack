package model;

import java.util.ArrayDeque;
import java.util.Deque;

public class Beacon {

    BeaconId beaconId;
    double x;
    double y;
    double txPower = -59; // default, RSSI value in 1 m
    double n = 2.0; // default, delay rate

    public final BeaconRssi rssi = new BeaconRssi();


    double distance;

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getDistance() {
        return distance;
    }



    public Beacon(BeaconId beaconId, double x, double y, double tx, double n) {
        this.beaconId = beaconId;
        this.x = x;
        this.y = y;
        this.txPower = tx;
        this.n = n;
    }

    public void setRssi(int rssiValue){
        rssi.feed(rssiValue);
    }

    public void setDistance(){
        distance = rssi.getDistanceFromRssi(txPower, n);
    }
}
