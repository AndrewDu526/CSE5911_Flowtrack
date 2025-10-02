package org.example.model.bundle;

import org.example.model.point.RawPoint;

import java.util.List;

public class Batch {
    public String batchId;
    public String deviceId;
    public String sessionId;
    public String source;
    public String coordRef;

    public List<RawPoint> points;

    public long startTimeMs;
    public long endTimeMs;
    public int count;

    public String buildingId;
    public String floorId;
    public String mapId;

    public Batch(String mapId, String floorId, String buildingId, int count, long endTimeMs,
                 long startTimeMs, List<RawPoint> points, String coordRef, String source,
                 String sessionId, String deviceId, String batchId) {
        this.mapId = mapId;
        this.floorId = floorId;
        this.buildingId = buildingId;
        this.count = count;
        this.endTimeMs = endTimeMs;
        this.startTimeMs = startTimeMs;
        this.points = points;
        this.coordRef = coordRef;
        this.source = source;
        this.sessionId = sessionId;
        this.deviceId = deviceId;
        this.batchId = batchId;
    }

    public Batch(){};
}