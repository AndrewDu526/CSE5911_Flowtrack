package org.example.service.preprocessing;

import org.example.model.bundle.Batch;
import org.example.model.point.RawPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DataPreprocessingModule {

    private final double samePointEps = 1e-6;
    private final double worstAcurracy = 5;
    private final double maxVelocity = 3;
    private final double mapMinX = -160;
    private final double mapMaxX = 240;
    private final double mapMinY = 0;
    private final double mapMaxY = 280;


    public List<RawPoint> merge(List<Batch> batches){
        List<RawPoint> points = new ArrayList<RawPoint>();
        if (batches == null) return points;
        for (Batch b : batches) {
            if (b == null || b.points  == null) continue;
            points.addAll(b.points);
        }
        return points;
    }

    public List<RawPoint> sort(List<RawPoint> in){
        if (in == null || in.isEmpty()) return in;
        List<RawPoint> points = new ArrayList<RawPoint>(in);
        Collections.sort(points, new Comparator<RawPoint>() {
            @Override public int compare(RawPoint a, RawPoint b) {
                return Long.compare(a.time, b.time);
            }
        });
        return points;
    }

    public List<RawPoint> deduplicate(List<RawPoint> in){
        if (in == null || in.isEmpty()) return in;

        List<RawPoint> byTs = new ArrayList<>();
        RawPoint best = null;
        long currentTs = Long.MIN_VALUE;

        for (RawPoint p : in) {
            if (p == null) continue;
            if (p.time != currentTs) {
                if (best != null) byTs.add(best);
                currentTs = p.time;
                best = p;
            } else {
                double a1 = (best != null) ? best.rms : null;
                double a2 = p.rms;
                boolean takeNew = a2 < a1;
                if (takeNew) best = p;
            }
        }
        if (best != null) byTs.add(best);

        List<RawPoint> out = new ArrayList<RawPoint>(byTs.size());
        RawPoint prev = null;
        for (RawPoint p : byTs) {
            if (prev != null) {
                if (p.time == prev.time && similarPoint(p, prev, samePointEps)) {continue;}
            }
            out.add(p);
            prev = p;
        }
        return out;
    }

    public List<RawPoint> filter(List<RawPoint> in){
        if (in == null || in.isEmpty()) return in;

        List<RawPoint> points = new ArrayList<RawPoint>(in.size());

        for (RawPoint p : in) {
            // outer bound
            if (!withinBounds(p.x, p.y)) continue;
            // accuracy/rms filter
            if (p.rms > worstAcurracy) continue;
            // speed limit
            if (p.velocity > maxVelocity) continue;
            points.add(p);
        }
        return points;
    }

    private boolean similarPoint(RawPoint a, RawPoint b, double eps) {
        return Math.abs(a.x - b.x) <= eps && Math.abs(a.y - b.y) <= eps;
    }

    private boolean withinBounds(double x, double y) {
        return x >= mapMinX && x <= mapMaxX && y >= mapMinY && y <= mapMaxY;
    }
}
