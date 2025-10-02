package org.example.repository.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.bundle.Batch;
import org.example.model.map.FloorMap;
import org.example.repository.LocalRepository;
import org.springframework.stereotype.Repository;
import java.nio.file.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Repository
public class LocalRepositoryImpl implements LocalRepository {

    private final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
