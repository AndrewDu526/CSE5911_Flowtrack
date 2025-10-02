package org.example.model.map;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;

public  class RoomGeom {
    public final Room room;
    public final Polygon polygon;
    public final PreparedGeometry prepared;
    public final Envelope envelope;

    public RoomGeom(Room room, Polygon polygon, PreparedGeometry prepared) {
        this.room = room;
        this.polygon = polygon;
        this.prepared = prepared;
        this.envelope = polygon.getEnvelopeInternal();
    }
}