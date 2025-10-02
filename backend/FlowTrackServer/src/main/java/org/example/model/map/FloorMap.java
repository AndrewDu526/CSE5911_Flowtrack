package org.example.model.map;

import java.util.List;

public class FloorMap {
    public String map_id;
    public String floor_id;
    public String building_id;

    public String name;
    public String description;

    public String units;
    public String crs;

    public List<Room> rooms;
    public List<Doorway> doorways;
    public Meta meta;
}
