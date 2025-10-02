package org.example.model.point;

public class TaggedPoint {
    public long t;
    public double x;
    public double y;
    public String room;     // room id, tag

    public TaggedPoint(long t, double x, double y, String room) {
        this.t = t;
        this.x = x;
        this.y = y;
        this.room = room;
    }
}
