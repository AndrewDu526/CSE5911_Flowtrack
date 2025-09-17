package org.example.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.TrackBatch;
import org.example.repository.localTempRepository;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@Repository
public class localTempRepositoryImpl implements localTempRepository {

    private static final Path OUTPUT_DIR = Paths.get("output").toAbsolutePath().normalize();
    private final ObjectMapper om = new ObjectMapper();

    public localTempRepositoryImpl() {

        try {
            if (!Files.exists(OUTPUT_DIR)){Files.createDirectories(OUTPUT_DIR);}
        } catch (Exception e) {
            throw new RuntimeException("fail to create output directory: " + OUTPUT_DIR, e);
        }
    }
    @Override
    public void savePackagesFromMobile(TrackBatch batch) throws Exception{

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
