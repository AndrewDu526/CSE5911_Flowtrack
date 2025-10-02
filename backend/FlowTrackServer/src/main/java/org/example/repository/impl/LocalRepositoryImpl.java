package org.example.repository.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.bundle.Batch;
import org.example.model.map.FloorMap;
import org.example.model.map.Meta;
import org.example.model.map.Room;
import org.example.model.map.Vertex;
import org.example.repository.LocalRepository;
import org.springframework.stereotype.Repository;

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Repository
public class LocalRepositoryImpl implements LocalRepository {

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void loadCsv()  throws Exception{

        String inputDir = "localRepository/input/csv";
        String outputDir = "localRepository/input/maps";

        File folder = new File(inputDir);
        if (!folder.exists() || !folder.isDirectory()) {throw new IllegalArgumentException("Input directory does not exist: " + inputDir);}

        File outFolder = new File(outputDir);
        if (!outFolder.exists()) {outFolder.mkdirs();}

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        if (files == null || files.length == 0) {System.out.println("No CSV files found in: " + inputDir);return;}

        for (File csvFile : files) {
            FloorMap floorMap = new FloorMap();
            try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
                String[] line;

                while ((line = reader.readNext()) != null) {
                    if (line.length == 0 || line[0].trim().equalsIgnoreCase("id")) {
                        break;
                    }
                    String key = line[0].trim();
                    String value = (line.length > 1) ? line[1].trim() : "";

                    switch (key) {
                        case "map_id": floorMap.map_id = value; break;
                        case "floor_id": floorMap.floor_id = value; break;
                        case "building_id": floorMap.building_id = value; break;
                        case "name": floorMap.name = value; break;
                        case "description": floorMap.description = value; break;
                        case "units": floorMap.units = value; break;
                        case "crs": floorMap.crs = value; break;
                        case "meta_version":
                            if (floorMap.meta == null) floorMap.meta = new Meta();
                            floorMap.meta.map_version = value;
                            break;
                        case "meta_author":
                            if (floorMap.meta == null) floorMap.meta = new Meta();
                            floorMap.meta.author = value;
                            break;
                    }
                }

                if (floorMap.meta == null) floorMap.meta = new Meta();
                floorMap.meta.created_at = LocalDate.now().toString();
                floorMap.rooms = new ArrayList<>();
                floorMap.doorways = new ArrayList<>();

                while ((line = reader.readNext()) != null) {
                    if (line.length == 0) continue;

                    Room room = new Room();
                    room.id = line[0];
                    room.name = line[1];
                    room.type = line[2];
                    room.vertices = new ArrayList<>();

                    for (int i = 3; i < line.length; i += 2) {
                        if (i + 1 >= line.length || line[i].isEmpty() || line[i+1].isEmpty()) break;
                        double x = Double.parseDouble(line[i]);
                        double y = Double.parseDouble(line[i + 1]);
                        room.vertices.add(new Vertex(x, y));
                    }
                    floorMap.rooms.add(room);
                }
            }

            String baseName = (floorMap.name != null && !floorMap.name.isEmpty())
                    ? floorMap.name
                    : csvFile.getName().replace(".csv", "");

            File outFile = new File(outputDir, baseName + ".json");
            mapper.writeValue(outFile, floorMap);

            System.out.println("Converted: " + csvFile.getName() + " → " + outFile.getAbsolutePath());
        }
    }

    @Override
    public FloorMap loadMapSetting(String dir, String fileName){

        if (dir == null || dir.isEmpty()) {throw new IllegalArgumentException("dir cannot be empty");}
        if (fileName == null || fileName.isEmpty()) {throw new IllegalArgumentException("fileName cannot be empty");}
        if (!fileName.toLowerCase().endsWith(".json")) {throw new IllegalArgumentException("fileName has to be .json file");}

        Path folder = Paths.get(dir);
        if (!Files.isDirectory(folder)) {throw new IllegalStateException("directory do not exist: " + folder.toAbsolutePath());}

        Path jsonPath = folder.resolve(fileName);
        if (!Files.isRegularFile(jsonPath)) {throw new IllegalStateException("file do not exist: " + jsonPath.toAbsolutePath());}

        try (BufferedReader reader = Files.newBufferedReader(jsonPath, StandardCharsets.UTF_8)) {
            // JSON file to FloorMap model
            return mapper.readValue(reader, FloorMap.class);
        } catch (IOException e) {
            throw new RuntimeException("read or map JSON fail: " + jsonPath.toAbsolutePath(), e);
        }
    }

    @Override
    public List<Batch> loadBatches(String dir, int size){
        if (dir == null || dir.trim().isEmpty()) {throw new IllegalArgumentException("dir can not be empty");}
        if (size <= 0) {throw new IllegalArgumentException("size has to larger > 0");}

        Path folder = Paths.get(dir);
        if (!Files.isDirectory(folder)) {throw new IllegalStateException("directory does not exist: " + folder.toAbsolutePath());}

        try {
            List<Path> files = new ArrayList<Path>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(folder, "*.json")) {
                for (Path p : ds) {
                    if (Files.isRegularFile(p)) files.add(p);
                }
            }
            if (files.isEmpty()) {return new ArrayList<Batch>();}
            files.sort(new Comparator<Path>() {
                @Override public int compare(Path a, Path b) {
                    try {
                        long ta = Files.getLastModifiedTime(a).toMillis();
                        long tb = Files.getLastModifiedTime(b).toMillis();
                        return Long.compare(ta, tb);
                    } catch (IOException e) { return 0; }
                }
            });

            int n = Math.min(size, files.size());
            List<Batch> out = new ArrayList<Batch>(n);

            for (int i = 0; i < n; i++) {
                Path f = files.get(i);
                try (BufferedReader reader = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    Batch tb = mapper.readValue(reader, Batch.class);
                    out.add(tb);
                } catch (IOException e) {
                    System.err.println("fail to read batch list, skipped: " + f.toAbsolutePath() + ". reason: " + e.getMessage());
                }
            }
            return out;
        } catch (IOException e) {
            throw new RuntimeException("fail to read directory: " + folder.toAbsolutePath(), e);
        }
    }

    @Override
    public void saveBatches(Batch batch) throws Exception{

        final Path OUTPUT_DIR = Paths.get("localRepository","input","maps").toAbsolutePath().normalize();
        final ObjectMapper om = new ObjectMapper();

        String fname = "batch_" + Instant.now().toEpochMilli() + ".json";
        String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(batch);// transfer TrackBatch to json

        Files.write(
                OUTPUT_DIR.resolve(fname),
                json.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
