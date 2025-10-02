package org.example.model.bundle.scriptuse;

import java.util.List;

/**
 * input package for scripts to generate heat map, traffic matrix and Spaghetti map
 */
public class ScriptInputBundle {

    /**
     * Meat data
     */
    public Meta meta;

    /**
     * static segment: time segment that stay in one room
     */
    public List<StaySegment> stay_segments;

    /**
     * room switch/transmission event
     */
    public List<RoomSwitch> r_transitions;

    /**
     * statistic of traffic on room transmission
     */
    public List<SwitchAggregated> rs_aggregated;

    public ScriptInputBundle(){};
}