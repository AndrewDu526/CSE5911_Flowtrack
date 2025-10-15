package org.example.service.tagging;

import org.example.model.point.RawPoint;
import org.example.model.map.RoomGeom;
import org.example.model.point.TaggedPoint;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
@Service
public class RoomMatchingModule {
    private final MapLoadingModule loader;
    private final double epsBoundary = 0.15;

    public RoomMatchingModule(MapLoadingModule loader) {this.loader = loader;}

    public TaggedPoint labelOnePoint(RawPoint point) {

        double x = point.x;
        double y = point.y;
        long t = point.time;

        Point p = loader.geometryFactory().createPoint(new Coordinate(x, y));

        @SuppressWarnings("unchecked")
        List<RoomGeom> candidates = loader.index().query(new Envelope(x, x, y, y));

        List<RoomGeom> hits = new ArrayList<>();
        boolean onBoundary = false;

        for (RoomGeom rg : candidates) {

            if (rg.prepared.covers(p)) {
                hits.add(rg);
            } else {
                double d = rg.polygon.distance(p);
                if (d <= epsBoundary) {
                    hits.add(rg);
                }
            }
        }
        RoomGeom roomGeom = hits.isEmpty() ? null : hits.get(0);
        if (hits.isEmpty()) {
            double minDist = Double.MAX_VALUE;
            RoomGeom nearest = null;

            // rooms
            for (RoomGeom g : loader.rooms().values()) {
                double d = g.polygon.distance(p);
                if (d < minDist) {
                    minDist = d;
                    nearest = g;
                }
            }

            if (minDist <= 0.3) {
                roomGeom = nearest;
            } else {
                roomGeom = null; // too far
            }
        }

        String roomId = (roomGeom != null) ? roomGeom.room.id : null;
        return new TaggedPoint(t, x, y, roomId);
    }

    public List<TaggedPoint> labelMultiPoints(List<RawPoint> points) {
        if (points == null || points.isEmpty()) return Collections.emptyList();
        List<TaggedPoint> out = new ArrayList<TaggedPoint>(points.size());
        for (RawPoint rp : points) {
            if (rp == null) continue;
            out.add(labelOnePoint(rp));
        }
        return out;
    }
}
