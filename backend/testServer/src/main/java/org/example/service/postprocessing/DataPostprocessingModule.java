package org.example.service.postprocessing;

import org.example.model.bundle.scriptuse.*;
import org.example.model.point.TaggedPoint;

import java.util.*;

public class DataPostprocessingModule {


    /** continually hit a new room counter */
    private final int L;
    /** continually hit a new room timer (ms) */
    private final long tauMs;
    /** minimum time stay in a room */
    private final long minDwellMs;

    public DataPostprocessingModule(int L, long tauMs, long minDwellMs) {
        this.L = Math.max(1, L);
        this.tauMs = Math.max(0, tauMs);
        this.minDwellMs = Math.max(0, minDwellMs);
    }

    public ScriptInputBundle buildBundle(List<TaggedPoint> points, Meta meta) {
        if (points == null) points = Collections.emptyList();

        // 1) do hysteresis
        List<StaySegment> segmentsDraft = buildSegmentsWithHysteresis(points);

        // 2) merge short segments
        List<StaySegment> segments = mergeShortStays(segmentsDraft);

        // 3) switch events
        List<RoomSwitch> switches = buildTransitionsFromSegments(segments);

        // 4) aggregated analysis
        List<SwitchAggregated> agg = aggregateSwitches(segments, switches);

        // 5) packaging
        ScriptInputBundle bundle = new ScriptInputBundle();
        bundle.meta = meta;
        bundle.stay_segments = segments;
        bundle.r_transitions = switches;
        bundle.rs_aggregated = agg;
        return bundle;
    }

    private List<StaySegment> buildSegmentsWithHysteresis(List<TaggedPoint> pts) {
        List<StaySegment> segments = new ArrayList<>();
        if (pts == null || pts.isEmpty()) return segments;

        String curRoom = firstNonNullRoom(pts);
        int curStartIdx = 0; // start_t


        String candidateRoom = null; // new room try to switch
        int countNew = 0;            // continual hits on new room
        long firstNewT = 0L;         // continual time in new room

        // linear scan
        for (int i = 0; i < pts.size(); i++) {
            TaggedPoint p = pts.get(i);

            // stay on current room
            if (eq(p.room, curRoom) || (p.room == null && curRoom == null)) {
                // clean candidates
                candidateRoom = null;
                countNew = 0;
                firstNewT = 0L;
                continue;
            }

            // diff room, try switching
            if (candidateRoom == null || !eq(candidateRoom, p.room)) {
                candidateRoom = p.room;
                countNew = 1;
                firstNewT = p.t;
            } else {
                countNew++;
            }

            // check hysteresis conditions
            boolean passByCount = (countNew >= L);
            boolean passByTime  = (p.t - firstNewT >= tauMs);
            if (passByCount || passByTime) {

                int endIdx = Math.max(i - 1, curStartIdx);
                segments.add(makeSegment(pts, curRoom, curStartIdx, endIdx));

                curRoom = candidateRoom;
                curStartIdx = i;

                candidateRoom = null;
                countNew = 0;
                firstNewT = 0L;
            }
        }

        segments.add(makeSegment(pts, curRoom, curStartIdx, pts.size() - 1));
        return segments;
    }

    /** create a segment */
    private StaySegment makeSegment(List<TaggedPoint> pts, String room, int startIdx, int endIdx) {
        StaySegment seg = new StaySegment();
        seg.room_id = room;
        seg.start_t = pts.get(startIdx).t;
        seg.end_t   = pts.get(endIdx).t;
        seg.duration_s = Math.max(0, (seg.end_t - seg.start_t) / 1000.0);
        return seg;
    }

    private String firstNonNullRoom(List<TaggedPoint> pts) {
        for (TaggedPoint p : pts) if (p.room != null) return p.room;
        return null;
    }

    /** string equal */
    private static boolean eq(String a, String b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private List<StaySegment> mergeShortStays(List<StaySegment> in) {
        if (in == null || in.size() <= 1 || minDwellMs <= 0) return in;

        List<StaySegment> s = new ArrayList<>(in);
        int i = 0;
        while (i < s.size()) {
            StaySegment cur = s.get(i);
            long durMs = (long) Math.round(cur.duration_s * 1000.0);

            // keep
            if (durMs >= minDwellMs) { i++; continue; }

            StaySegment prev = (i > 0) ? s.get(i - 1) : null;
            StaySegment next = (i + 1 < s.size()) ? s.get(i + 1) : null;

            // case 1: A-B-A，B is regard as A
            if (prev != null && next != null && eq(prev.room_id, next.room_id)) {
                prev.end_t = next.end_t;
                prev.duration_s = Math.max(0, (prev.end_t - prev.start_t) / 1000.0);
                s.remove(i + 1);
                s.remove(i);
                i = Math.max(0, i - 1);
                continue;
            }

            // case 2: join prev
            if (prev != null) {
                prev.end_t = cur.end_t;
                prev.duration_s = Math.max(0, (prev.end_t - prev.start_t) / 1000.0);
                s.remove(i);
                i = Math.max(0, i - 1);
                continue;
            }

            // case 3: join next
            if (next != null) {
                next.start_t = cur.start_t;
                next.duration_s = Math.max(0, (next.end_t - next.start_t) / 1000.0);
                s.remove(i);
                continue;
            }

            // case 4: remove itself
            s.remove(i);
        }
        return s;
    }

    private List<RoomSwitch> buildTransitionsFromSegments(List<StaySegment> segs) {
        List<RoomSwitch> out = new ArrayList<>();
        if (segs == null || segs.size() <= 1) return out;

        for (int k = 0; k + 1 < segs.size(); k++) {
            StaySegment a = segs.get(k);
            StaySegment b = segs.get(k + 1);
            if (eq(a.room_id, b.room_id)) continue; // same room

            RoomSwitch sw = new RoomSwitch();
            sw.from = a.room_id;
            sw.to   = b.room_id;
            sw.at   = b.start_t;
            out.add(sw);
        }
        return out;
    }

    private List<SwitchAggregated> aggregateSwitches(List<StaySegment> segs, List<RoomSwitch> switches) {
        if (segs == null || segs.size() <= 1) return Collections.emptyList();

        // key = from + "||" + to
        Map<String, SwitchAggregated> map = new LinkedHashMap<>();

        for (int i = 0; i + 1 < segs.size(); i++) {
            StaySegment a = segs.get(i);
            StaySegment b = segs.get(i + 1);

            if (eq(a.room_id, b.room_id)) continue;

            String from = a.room_id;
            String to   = b.room_id;

            // drop unknown room
            // if (from == null || to == null) continue;

            // travel time
            double travelS = Math.max(0, (b.start_t - a.end_t) / 1000.0);

            String key = from + "||" + to;
            SwitchAggregated agg = map.get(key);
            if (agg == null) {
                agg = new SwitchAggregated();
                agg.from = from;
                agg.to = to;
                agg.count_trips = 0;
                agg.total_travel_s = 0.0;
                agg.avg_travel_s = 0.0;
                map.put(key, agg);
            }

            agg.count_trips += 1;
            agg.total_travel_s += travelS;
            agg.avg_travel_s = (agg.count_trips > 0) ? (agg.total_travel_s / agg.count_trips) : 0.0;
        }

        return new ArrayList<>(map.values());
    }
}
