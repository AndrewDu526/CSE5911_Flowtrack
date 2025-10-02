package org.example.model.point;

// model to store single location point
public class RawPoint {
    public long   time;
    public double x;
    public double y;
    public double velocity;
    public boolean accepted;
    public int anchorsUsed;
    public double rms;
    public double dt;

    public RawPoint(long time, double x, double y, double velocity, boolean accepted, int anchorsUsed, double rms, double dt) {
        this.time = time;
        this.x = x;
        this.y = y;
        this.velocity = velocity;
        this.accepted = accepted;
        this.anchorsUsed = anchorsUsed;
        this.rms = rms;
        this.dt = dt;
    }

    public RawPoint(){};
}

