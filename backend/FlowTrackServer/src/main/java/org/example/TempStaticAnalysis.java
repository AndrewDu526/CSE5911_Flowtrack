package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.bundle.Batch;
import org.example.model.bundle.scriptuse.Meta;
import org.example.model.bundle.scriptuse.ScriptInputBundle;
import org.example.model.point.RawPoint;
import org.example.model.point.TaggedPoint;
import org.example.repository.LocalRepository;
import org.example.repository.impl.LocalRepositoryImpl;
import org.example.service.postprocessing.DataPostprocessingModule;
import org.example.service.preprocessing.DataPreprocessingModule;
import org.example.service.tagging.MapLoadingModule;
import org.example.service.tagging.RoomMatchingModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TempStaticAnalysis {

    public static void main(String[] args) throws Exception {
        new TempStaticAnalysis().run(args);
    }

    void run(String[] args) throws Exception {

    Meta meta = new Meta();
    LocalRepository repo = new LocalRepositoryImpl();
    DataPreprocessingModule preprocessor = new DataPreprocessingModule();
    MapLoadingModule mapLoader = new MapLoadingModule(repo);
    RoomMatchingModule roomMatcher = new RoomMatchingModule(mapLoader);
    DataPostprocessingModule postprocessor = new DataPostprocessingModule(3, 1500, 10000);

    List<Batch> batches = repo.loadBatches("localRepository/input/batches", 8);

    List<RawPoint> rawPoints = preprocessor.merge(batches);
    List<RawPoint> rawPointsSorted = preprocessor.sort(rawPoints);
    List<RawPoint> rawPointsDeduplicated = preprocessor.deduplicate(rawPointsSorted);
    List<RawPoint> rawPointsFiltered = preprocessor.filter(rawPointsDeduplicated);

    List<TaggedPoint> taggedPoints = roomMatcher.labelMultiPoints(rawPointsFiltered);

    ScriptInputBundle input = postprocessor.buildBundle(taggedPoints, meta);

    ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    String json = om.writeValueAsString(input);

    Path outDir = Paths.get("localRepository", "output", "scriptsInputBundle");
    Files.createDirectories(outDir);

    String fileName = "test_output_bundles.json";

    Path outFile = outDir.resolve(fileName);

    Files.write(outFile, json.getBytes());

    System.out.println("Wrote bundle to: " + outFile.toAbsolutePath());
    System.out.println("Working dir was: " + System.getProperty("user.dir"));
    }
}
