package com.example.prototypeapp.data.model;

import java.util.ArrayList;
import java.util.List;

public class TrackBatch {

    public String batchId;
    public String deviceId;
    public String sessionId;
    public String source;
    public String coordRef;

    public List<TrackPoint> points;

    public long startTimeMs;
    public long endTimeMs;
    public int count;

    public final String buildingId;
    public final String floorId;
    public final String mapId;

    public TrackBatch(String mapId, String floorId, String buildingId, int count, long endTimeMs,
                      long startTimeMs, List<TrackPoint> points, String coordRef, String source,
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
}