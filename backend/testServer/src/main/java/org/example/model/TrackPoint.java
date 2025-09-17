package org.example.model;

// model to store single location point
public class TrackPoint {
    public long tWall;       // System.currentTimeMillis()
    public long tElapsedNs;  // SystemClock.elapsedRealtimeNanos()
    public double lat, lon; // latitude and longitude
    public float acc; // accuracy
    public String provider; // tech provide: GPS/BLT/WIFI...
    public String source; // tech source: FLP
    public String priority; // Fine location/Coarse location/Balanced
    public String floor; // TODO: floor
}

