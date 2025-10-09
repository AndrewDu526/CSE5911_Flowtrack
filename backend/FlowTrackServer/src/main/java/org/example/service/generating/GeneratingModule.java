package org.example.service.generating;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * GeneratingModule
 * --------------------------------------------------
 * This class wraps the execution of Python visualization scripts:
 *   - traffic_matrix.py: generates room-to-room transition matrices.
 *   - traffic_heatmap.py: generates room dwell-time heatmaps.
 *
 * Each method receives three arguments:
 *   1. mapPath    - JSON file containing map/room geometry.
 *   2. bundlePath - JSON file containing processed stay/transition data.
 *   3. outputDir  - directory where visualization and JSON results are saved.
 *
 * You can call these methods from a launcher or any service layer.
 */
public class GeneratingModule {

    // Python executable name (can be changed to "python3" or an absolute path)
    private static final String PYTHON_EXECUTABLE = "python";

    // Directory where all Python scripts are located
    private static final String SCRIPT_DIR = "src/main/resources/scripts/";

    // Paths to each script
    private static final String TRAFFIC_MATRIX_SCRIPT = SCRIPT_DIR + "traffic_matrix_generator.py";
    private static final String HEATMAP_SCRIPT = SCRIPT_DIR + "heat_map_generator.py";
    private static final String SPAGHETTI_SCRIPT = SCRIPT_DIR + "spaghetti_map_generator.py";

    // Default repository structure
    private final String BASE_DIR;
    private final String MAP_PATH;
    private final String BUNDLE_PATH;
    private final String MATRIX_OUTPUT;
    private final String HEATMAP_OUTPUT;
    private final String SPAGHETTI_OUTPUT;

    /**
     * Default constructor: initializes all paths automatically
     * based on your existing local repository structure.
     */
    public GeneratingModule() {
        this.BASE_DIR = "D:\\workspace\\monorepo\\FlowTrack\\backend\\FlowTrackServer\\localRepository";
        this.MAP_PATH = BASE_DIR + "\\input\\maps\\test_map_001.json";
        this.BUNDLE_PATH = BASE_DIR + "\\output\\scriptsInputBundle\\test_output_bundles.json";
        this.MATRIX_OUTPUT = BASE_DIR + "\\output\\report\\trafficmatrix";
        this.HEATMAP_OUTPUT = BASE_DIR + "\\output\\report\\heatmap";
        this.SPAGHETTI_OUTPUT = BASE_DIR + "\\output\\report\\spaghettimap";
    }

    /**
     * Overloaded constructor for dynamic base directory.
     */
    public GeneratingModule(String baseDir) {
        this.BASE_DIR = baseDir;
        this.MAP_PATH = baseDir + "\\input\\maps\\test_map_001.json";
        this.BUNDLE_PATH = baseDir + "\\output\\scriptsInputBundle\\test_output_bundles.json";
        this.MATRIX_OUTPUT = baseDir + "\\output\\report\\trafficmatrix";
        this.HEATMAP_OUTPUT = baseDir + "\\output\\report\\heatmap";
        this.SPAGHETTI_OUTPUT = baseDir + "\\output\\report\\spaghetti";
    }

    /**
     * Run all three visualization scripts in sequence using preset paths.
     */
    public void generate() {
        System.out.println("=== Running all visualization generators ===");
        trafficMatrixGenerator(MAP_PATH, BUNDLE_PATH, MATRIX_OUTPUT);
        heatMapGenerator(MAP_PATH, BUNDLE_PATH, HEATMAP_OUTPUT);
        spaghettiMapGenerator(MAP_PATH, BUNDLE_PATH, SPAGHETTI_OUTPUT);
        System.out.println("=== All visualizations generated successfully ===");
    }


    /**
     * Run traffic_matrix.py to generate transition and travel-time matrices.
     *
     * @param mapPath    Path to map JSON file.
     * @param bundlePath Path to postprocessed bundle JSON file.
     * @param outputDir  Directory for generated reports.
     */
    public void trafficMatrixGenerator(String mapPath, String bundlePath, String outputDir) {
        runPythonScript("traffic_matrix.py", TRAFFIC_MATRIX_SCRIPT, mapPath, bundlePath, outputDir);
    }


    /**
     * Run traffic_heatmap.py to generate room dwell-time heatmap visualization.
     *
     * @param mapPath    Path to map JSON file.
     * @param bundlePath Path to postprocessed bundle JSON file.
     * @param outputDir  Directory for generated reports.
     */
    public void heatMapGenerator(String mapPath, String bundlePath, String outputDir) {
        runPythonScript("traffic_heatmap.py", HEATMAP_SCRIPT, mapPath, bundlePath, outputDir);
    }

    /**
     * Run spaghetti_map_generator.py to generate a spaghetti (flow) diagram.
     *
     * @param mapPath    Path to map JSON file.
     * @param bundlePath Path to transition JSON file (from traffic_matrix.py output).
     * @param outputDir  Directory for generated PNG output.
     */
    public void spaghettiMapGenerator(String mapPath, String bundlePath, String outputDir) {
        runPythonScript("spaghetti_map_generator.py", SPAGHETTI_SCRIPT, mapPath, bundlePath, outputDir);
    }

    /**
     * Common internal method for running Python scripts via ProcessBuilder.
     *
     * @param scriptName Logical name of the script (for logs).
     * @param scriptPath Absolute or relative path to the .py file.
     * @param mapPath    Input map JSON file.
     * @param bundlePath Input bundle JSON file.
     * @param outputDir  Output directory for generated results.
     */
    private void runPythonScript(String scriptName, String scriptPath,
                                 String mapPath, String bundlePath, String outputDir) {

        List<String> command = new ArrayList<>();
        command.add(PYTHON_EXECUTABLE);
        command.add(scriptPath);
        command.add(mapPath);
        command.add(bundlePath);
        command.add(outputDir);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.directory(new File(System.getProperty("user.dir"))); // set working directory to project root

        try {
            System.out.println("----------------------------------------------------");
            System.out.println("Running " + scriptName + " ...");
            System.out.println("Map Path: " + mapPath);
            System.out.println("Bundle Path: " + bundlePath);
            System.out.println("Output Dir: " + outputDir);

            Process process = pb.start();

            // Read and forward Python output to Java console
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[PY] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("[" + scriptName + "] Finished successfully.");
            } else {
                System.err.println("[" + scriptName + "] Exited with code: " + exitCode);
            }

        } catch (IOException e) {
            System.err.println("I/O error while running " + scriptName + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Python process interrupted: " + e.getMessage());
        }

        System.out.println("----------------------------------------------------\n");
    }
}
