package model;

// model to store single location point
public class TrackPoint {
    public long tWall;       // System.currentTimeMillis()
    public long tElapsedNs;  // SystemClock.elapsedRealtimeNanos()
    public double lat, lon; // latitude and longitude
    public String provider; // tech provide: GPS/BLT/WIFI...
    public String source; // tech source: FLP
}

