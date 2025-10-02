package org.example.service.tagging;

import org.example.model.map.FloorMap;
import org.example.model.map.Room;
import org.example.model.map.RoomGeom;
import org.example.model.map.Vertex;
import org.example.repository.LocalRepository;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class MapLoadingModule {


    private final LocalRepository repo;
    private final GeometryFactory gf = new GeometryFactory();
    private final PreparedGeometryFactory pFactory = new PreparedGeometryFactory();

    private FloorMap floorMap;
    private Map<String, RoomGeom> byId;
    private STRtree rtree;

    public MapLoadingModule(LocalRepository repo){
        this.repo = repo;
        buildOnce();
    }

    //@PostConstruct
    public void init() {buildOnce();}
    private void buildOnce() {
        // 1) read FloorMap
        this.floorMap = repo.loadMapSetting("localRepository/input/maps", "test_map_001.json");

        // 2) Room -> Polygon
        this.byId = new HashMap<String, RoomGeom>();
        this.rtree = new STRtree();

        if (floorMap.rooms == null || floorMap.rooms.isEmpty()) {
            throw new IllegalStateException("rooms not find in map");
        }

        for (Room r : floorMap.rooms) {

            Polygon polygon = room2Polygon(r.vertices);

            // expand（>0）/reduce（<0）；quadrantSegments=1；
            // room boundary expand 0.5 meters
            double roomBufferM = 0.5;
            polygon = (Polygon) polygon.buffer(roomBufferM, 1, BufferParameters.CAP_FLAT);

            if (!polygon.isValid()) {
                throw new IllegalArgumentException("invalid polygon id=" + r.id);
            }

            PreparedGeometry pg = pFactory.create(polygon);
            RoomGeom rg = new RoomGeom(r, polygon, pg);

            byId.put(r.id, rg);
            rtree.insert(rg.envelope, rg);
        }

        rtree.build();
    }

    /** List<Vertex> to closed JTS Polygon */
    private Polygon room2Polygon(List<Vertex> vs) {
        if (vs == null || vs.size() < 3) {
            throw new IllegalArgumentException("room vertex less than 3 vs=" + vs);
        }
        Coordinate[] coords = new Coordinate[vs.size() + 1];
        for (int i = 0; i < vs.size(); i++) {
            Vertex p = vs.get(i);
            coords[i] = new Coordinate(p.x, p.y);
        }
        // closed
        coords[vs.size()] = new Coordinate(vs.get(0).x, vs.get(0).y);
        LinearRing shell = gf.createLinearRing(coords);
        return gf.createPolygon(shell, null);
    }

    // read only service
    public GeometryFactory geometryFactory() { return gf; }

    /** space index */
    public STRtree index() { return rtree; }

    /** get room by id */
    public RoomGeom roomById(String id) { return byId.get(id); }

    /** get all rooms */
    public Map<String, RoomGeom> rooms() { return byId; }

    /** raw floor map */
    public FloorMap floorMap() { return floorMap; }
}
