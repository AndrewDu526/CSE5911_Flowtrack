package org.example.model.bundle.scriptuse;

public class StaySegment {
    /** room id, unknown = null*/
    public String room_id;

    /** start time stamp (ms)*/
    public long start_t;

    /** end time stamp (ms)*/
    public long end_t;

    /** 段时长（秒）= (end_t - start_t)/1000 */
    public double duration_s;

    public StaySegment(){};
}
