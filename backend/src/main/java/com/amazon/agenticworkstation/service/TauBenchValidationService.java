package com.amazon.agenticworkstation.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service that executes the compute_complexity.ipynb notebook programmatically
 * to perform validation steps against the remote tau-bench API.
 */
@Service
public class TauBenchValidationService {
    private static final Logger log = LoggerFactory.getLogger(TauBenchValidationService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> run(String step, Path repositoryPath) throws IOException {
        log.info("Starting TauBench validation for step='{}' with repository path='{}'", step, repositoryPath);
        
        if (repositoryPath == null) {
            log.error("Repository path is null");
            throw new IOException("Repository path not set");
        }
        if (!Files.exists(repositoryPath)) {
            log.error("Repository path does not exist: {}", repositoryPath);
            throw new IOException("Repository path does not exist: " + repositoryPath);
        }
        
        Path taskFile = repositoryPath.resolve("task.json");
        if (!Files.exists(taskFile)) {
            log.error("task.json not found at: {}", taskFile);
            throw new IOException("task.json not found at " + taskFile);
        }
        log.debug("Found task.json at: {}", taskFile);

        Map<String, Object> result = new HashMap<>();
        result.put("step", step);
        result.put("timestamp", Instant.now().toString());
        log.debug("Initialized result map with step='{}' and timestamp", step);

        try {
            log.debug("Loading notebook and Python script resources from classpath");
            // Get the notebook and python script paths
            ClassPathResource notebookResource = new ClassPathResource("compute_complexity.ipynb");
            ClassPathResource pythonScriptResource = new ClassPathResource("run_notebook.py");
            
            if (!notebookResource.exists()) {
                log.error("compute_complexity.ipynb not found in classpath");
                throw new IOException("compute_complexity.ipynb not found in classpath");
            }
            if (!pythonScriptResource.exists()) {
                log.error("run_notebook.py not found in classpath");
                throw new IOException("run_notebook.py not found in classpath");
            }
            log.debug("Successfully located required classpath resources");

            String notebookPath = notebookResource.getFile().getAbsolutePath();
            String pythonScriptPath = pythonScriptResource.getFile().getAbsolutePath();
            String taskPath = taskFile.toAbsolutePath().toString();
            log.info("Resolved paths - Notebook: '{}', Python script: '{}', Task: '{}'", 
                    notebookPath, pythonScriptPath, taskPath);
            
            // Create output path for executed notebook
            Path outputNotebook = repositoryPath.resolve(step + "_executed_notebook.ipynb");
            log.debug("Output notebook will be saved to: {}", outputNotebook);
            
            log.info("Executing notebook for step='{}' using task='{}'", step, taskPath);
            
            // Build the command to execute the Python script
            ProcessBuilder pb = new ProcessBuilder(
                "python", pythonScriptPath,
                notebookPath,
                taskPath,
                step,
                "--output", outputNotebook.toString(),
                "--json-output"
            );
            log.debug("Built ProcessBuilder command: {}", pb.command());
            
            pb.redirectErrorStream(true);
            pb.directory(repositoryPath.toFile()); // Set working directory
            log.debug("Set working directory to: {}", repositoryPath);
            
            // Execute the process
            log.info("Starting Python notebook execution process");
            Process process = pb.start();
            log.debug("Process started with PID: {}", process.pid());
            
            // Capture output
            log.debug("Starting to capture process output");
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    lineCount++;
                    log.debug("Notebook output line {}: {}", lineCount, line);
                }
                log.info("Captured {} lines of output from notebook execution", lineCount);
            }
            
            // Wait for completion with timeout
            log.info("Waiting for notebook execution to complete (timeout: 10 minutes)");
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                log.error("Notebook execution timed out after 10 minutes, destroying process");
                process.destroyForcibly();
                throw new IOException("Notebook execution timed out after 10 minutes");
            }
            
            int exitCode = process.exitValue();
            String outputStr = output.toString().trim();
            log.info("Notebook execution completed with exit code: {}", exitCode);
            log.debug("Full output length: {} characters", outputStr.length());
            
            if (exitCode == 0) {
                log.info("Notebook execution succeeded, attempting to parse JSON output");
                // Try to parse JSON output from the Python script
                try {
                    Map<String, Object> notebookResult = mapper.readValue(outputStr, new TypeReference<>(){});
                    log.info("Successfully parsed JSON output with {} keys", notebookResult.size());
                    log.debug("Parsed JSON keys: {}", notebookResult.keySet());
                    
                    result.put("success", true);
                    result.put("notebook_result", notebookResult);
                    result.put("executed_notebook", outputNotebook.toString());
                    
                    // Extract specific results based on step
                    if (notebookResult.containsKey("result_file")) {
                        result.put("result_file", notebookResult.get("result_file"));
                        log.debug("Found result_file: {}", notebookResult.get("result_file"));
                    }
                    if (notebookResult.containsKey("response_file")) {
                        result.put("response_file", notebookResult.get("response_file"));
                        log.debug("Found response_file: {}", notebookResult.get("response_file"));
                    }
                    if (notebookResult.containsKey("evaluation_file")) {
                        result.put("evaluation_file", notebookResult.get("evaluation_file"));
                        log.debug("Found evaluation_file: {}", notebookResult.get("evaluation_file"));
                    }
                    
                    // Include any data that was parsed
                    if (notebookResult.containsKey("result_data")) {
                        result.put("result_data", notebookResult.get("result_data"));
                        log.debug("Included result_data in response");
                    }
                    if (notebookResult.containsKey("response_data")) {
                        result.put("response_data", notebookResult.get("response_data"));
                        log.debug("Included response_data in response");
                    }
                    if (notebookResult.containsKey("evaluation_data")) {
                        result.put("evaluation_data", notebookResult.get("evaluation_data"));
                        log.debug("Included evaluation_data in response");
                    }
                    
                    // Check for generated image files and include their paths
                    Path taskDir = repositoryPath.getParent() != null ? repositoryPath.getParent() : repositoryPath;
                    Path outputImage = taskDir.resolve("output.png");
                    if (Files.exists(outputImage)) {
                        result.put("output_image_path", outputImage.toString());
                        result.put("has_visualization", true);
                        log.debug("Found output.png at: {}", outputImage);
                    }
                    
                    // Also check in the task directory itself
                    Path taskDirImage = repositoryPath.resolve("output.png");
                    if (Files.exists(taskDirImage)) {
                        result.put("output_image_path", taskDirImage.toString());
                        result.put("has_visualization", true);
                        log.debug("Found output.png in task directory: {}", taskDirImage);
                    }
                    
                } catch (Exception e) {
                    // If JSON parsing fails, treat as text output
                    log.warn("Could not parse notebook output as JSON, treating as text: {}", e.getMessage());
                    log.debug("Raw output that failed JSON parsing: {}", outputStr);
                    result.put("success", true);
                    result.put("output", outputStr);
                    result.put("executed_notebook", outputNotebook.toString());
                }
            } else {
                log.error("Notebook execution failed with exit code: {}", exitCode);
                log.error("Error output: {}", outputStr);
                result.put("success", false);
                result.put("error", "Notebook execution failed with exit code " + exitCode);
                result.put("output", outputStr);
            }
            
        } catch (InterruptedException e) {
            log.error("Notebook execution was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            result.put("success", false);
            result.put("error", "Notebook execution was interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to execute notebook for step '{}': {}", step, e.getMessage(), e);
            result.put("success", false);
            result.put("error", "Failed to execute notebook: " + e.getMessage());
        }

        log.info("TauBench validation completed for step='{}' with success={}", 
                step, result.get("success"));
        return result;
    }
}
