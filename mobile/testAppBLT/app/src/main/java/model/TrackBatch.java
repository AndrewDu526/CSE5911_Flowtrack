package model;

import java.util.List;

public class TrackBatch {
    public String batchId;    // unique batchId for each package
    public String batchName;
    public List<TrackPoint> locations; // list of track point
    public Employee employee; // employee who be tracked
    public EmployeeDevice device; // device which execute app to track
}