package com.amazon.agenticworkstation.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amazon.agenticworkstation.dto.CacheUpdateRequest;
import com.amazon.agenticworkstation.service.TaskCacheService;
import com.amazon.agenticworkstation.service.TauBenchValidationService;

@RestController
@RequestMapping("/cache")
public class CacheController {
    private static final Logger log = LoggerFactory.getLogger(CacheController.class);
    
    private final TaskCacheService cacheService;
    private final TauBenchValidationService validationService;

    public CacheController(TaskCacheService cacheService, TauBenchValidationService validationService) {
        this.cacheService = cacheService;
        this.validationService = validationService;
    }

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody CacheUpdateRequest req) {
        log.info("Cache update request received: {}", req);
        
        // Debug: Check if edges contain escaped connection strings
        if (req.getEdges() != null) {
            for (int i = 0; i < req.getEdges().size(); i++) {
                Map<String, Object> edge = req.getEdges().get(i);
                Object connection = edge.get("connection");
                System.out.println("DEBUG Edge " + i + " connection type: " + (connection != null ? connection.getClass().getSimpleName() : "null"));
                System.out.println("DEBUG Edge " + i + " connection value: " + connection);
            }
        }
        
        try {
            cacheService.applyUpdate(req);
            log.debug("Cache update applied successfully");
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "aggregated", cacheService.aggregatedJson()
            ));
        } catch (Exception e) {
            log.error("Failed to update cache: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage(),
                "status", "failed"
            ));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> current() {
        log.info("Get current cache request received");
        try {
            String aggregated = cacheService.aggregatedJson();
            log.debug("Current cache retrieved successfully, length: {} characters", aggregated.length());
            return ResponseEntity.ok(aggregated);
        } catch (IOException e) {
            log.error("Failed to get current cache: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/save-file")
    public ResponseEntity<?> saveFile(@RequestBody(required = false) Map<String,String> body) {
        String dir = body != null ? body.get("directory") : null;
        log.info("Save file request received with directory: '{}'", dir);
        try {
            String json = cacheService.writeTaskJsonToRepository(dir);
            String effectiveDir = dir != null ? dir : cacheService.getRepositoryPath();
            log.info("Task JSON saved successfully to directory: '{}'", effectiveDir);
            return ResponseEntity.ok(Map.of(
                    "status", "saved",
                    "directory", effectiveDir,
                    "taskJson", json
            ));
        } catch (IOException e) {
            log.error("Failed to save task JSON to directory '{}': {}", dir, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/validate/{step}")
    public ResponseEntity<?> validateStep(@PathVariable String step, @RequestBody(required = false) Map<String,String> body) {
        String dir = body != null ? body.get("directory") : null;
        log.info("Validation request received for step: '{}' with directory: '{}'", step, dir);
        
        try {
            String effectiveDir = dir != null ? dir : cacheService.getRepositoryPath();
            log.debug("Using effective directory for validation: '{}'", effectiveDir);
            
            Map<String,Object> result;
            // Handle web directories differently since they're not real filesystem paths
            if (effectiveDir != null && effectiveDir.startsWith("web:")) {
                log.info("Web directory detected for validation, returning mock result: '{}'", effectiveDir);
                // For web directories, return a mock validation result
                result = Map.of(
                    "step", step,
                    "status", "skipped",
                    "message", "Validation skipped for web directory",
                    "isWebDirectory", true
                );
            } else if (effectiveDir != null) {
                result = validationService.run(step, Path.of(effectiveDir));
            } else {
                throw new IllegalArgumentException("No directory available for validation");
            }
            
            log.info("Validation completed successfully for step: '{}', result keys: {}", 
                    step, result != null ? result.keySet() : "null");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Validation failed for step '{}' in directory '{}': {}", step, dir, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "step", step,
                    "status", "failed",
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/validate/{step}")
    public ResponseEntity<?> validateStepGet(@PathVariable String step, @RequestParam(required = false) String directory) {
        try {
            String effectiveDir = directory != null ? directory : cacheService.getRepositoryPath();
            
            Map<String,Object> result;
            // Handle web directories differently since they're not real filesystem paths
            if (effectiveDir != null && effectiveDir.startsWith("web:")) {
                // For web directories, return a mock validation result
                result = Map.of(
                    "step", step,
                    "status", "skipped",
                    "message", "Validation skipped for web directory",
                    "isWebDirectory", true
                );
            } else if (effectiveDir != null) {
                result = validationService.run(step, Path.of(effectiveDir));
            } else {
                throw new IllegalArgumentException("No directory available for validation");
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "step", step,
                    "status", "failed",
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/load")
    public ResponseEntity<?> loadExisting(@RequestBody Map<String,String> body) {
        log.info("Load existing task request received with body: {}", body);
        
        String dir = body.get("directory");
        log.debug("Extracted directory from request: '{}'", dir);
        
        if (dir == null || dir.isBlank()) {
            log.warn("Load request failed: directory parameter is null or blank");
            return ResponseEntity.badRequest().body(Map.of("error","directory required"));
        }
        
        try {
            log.debug("Applying cache update with repository path: '{}'", dir);
            cacheService.applyUpdate(new CacheUpdateRequest() {{ setRepositoryPath(dir); }});
            
            log.debug("Loading existing task from path: '{}'", dir);
            // Handle web directories differently since they're not real filesystem paths
            if (dir.startsWith("web:")) {
                log.info("Web directory detected, skipping filesystem operations: '{}'", dir);
                // For web directories, we don't load from filesystem, just use current state
            } else {
                cacheService.loadExistingTask(Path.of(dir));
            }
            
            String aggregatedJson = cacheService.aggregatedJson();
            log.info("Successfully loaded task from directory: '{}', aggregated JSON length: {} characters", 
                    dir, aggregatedJson != null ? aggregatedJson.length() : 0);
            
            // Add a note if this is a web directory
            if (dir.startsWith("web:")) {
                log.warn("Note: Web directory path detected ('{}') - file operations may be limited. " +
                        "Consider using manual path entry in the UI for full functionality.", dir);
            }
            
            return ResponseEntity.ok(Map.of(
                    "status","loaded",
                    "aggregated", aggregatedJson
            ));
        } catch (Exception e) {
            log.error("Failed to load existing task from directory '{}': {}", dir, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Clear cache for specific user and task
     * This ensures old task data doesn't interfere with new imports
     */
    @PostMapping("/clear")
    public ResponseEntity<?> clearCache(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String taskId = body.get("taskId");
        
        log.info("Clear cache request received for userId: '{}', taskId: '{}'", userId, taskId);
        
        try {
            if (userId == null || userId.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }
            
            // If taskId is provided, clear specific task cache, otherwise clear all for user
            if (taskId != null && !taskId.isBlank()) {
                cacheService.clearCache(userId, taskId);
                log.info("Successfully cleared cache for user '{}' task '{}'", userId, taskId);
            } else {
                cacheService.clearUserCache(userId);
                log.info("Successfully cleared all cache entries for user '{}'", userId);
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "cleared",
                "message", "Cache cleared successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to clear cache for user '{}' task '{}': {}", userId, taskId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/policy")
    public ResponseEntity<?> getPolicy(@RequestParam(required = false) String directory,
                                      @RequestParam String env,
                                      @RequestParam int interfaceNum) {
        log.info("Policy request received - directory: '{}', env: '{}', interface: {}", directory, env, interfaceNum);
        
        try {
            String effectiveDir = directory != null ? directory : cacheService.getRepositoryPath();
            
            if (effectiveDir == null || effectiveDir.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No directory specified"));
            }
            
            // Handle web directories
            if (effectiveDir.startsWith("web:")) {
                log.warn("Web directory detected, cannot read policy file: '{}'", effectiveDir);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot read policy file from web directory",
                    "webMode", true,
                    "suggestion", "Use manual path entry to specify actual local directory"
                ));
            }
            
            // Build policy path: directory/../../../envs/{env}/tools/interface_{interfaceNum}/policy.md
            Path repoPath = Paths.get(effectiveDir);
            Path basePath = repoPath.getParent().getParent().getParent(); // Go up 3 levels
            Path policyPath = basePath.resolve("envs")
                                    .resolve(env)
                                    .resolve("tools")
                                    .resolve("interface_" + interfaceNum)
                                    .resolve("policy.md");
            
            log.debug("Constructed policy path: '{}'", policyPath);
            
            if (!Files.exists(policyPath)) {
                log.warn("Policy file not found at path: '{}'", policyPath);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Policy file not found",
                    "path", policyPath.toString()
                ));
            }
            
            String content = Files.readString(policyPath);
            log.info("Successfully loaded policy file from: '{}', content length: {} characters", 
                    policyPath, content.length());
            
            return ResponseEntity.ok(Map.of(
                "content", content,
                "path", policyPath.toString(),
                "env", env,
                "interface", interfaceNum
            ));
            
        } catch (Exception e) {
            log.error("Failed to load policy file for env '{}', interface {}: {}", env, interfaceNum, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<?> getImage(@PathVariable String filename, @RequestParam(required = false) String directory) {
        try {
            String effectiveDir = directory != null ? directory : cacheService.getRepositoryPath();
            
            if (effectiveDir == null) {
                throw new IllegalArgumentException("No directory available for image retrieval");
            }
            
            Path imagePath;
            if (effectiveDir.startsWith("web:")) {
                log.warn("Cannot serve images from web directory: {}", effectiveDir);
                return ResponseEntity.notFound().build();
            } else {
                // Check both the directory itself and parent directory
                Path baseDir = Path.of(effectiveDir);
                imagePath = baseDir.resolve(filename);
                
                if (!Files.exists(imagePath)) {
                    // Try parent directory
                    Path parentDir = baseDir.getParent();
                    if (parentDir != null) {
                        imagePath = parentDir.resolve(filename);
                    }
                }
            }
            
            if (!Files.exists(imagePath)) {
                log.warn("Image file not found: {}", imagePath);
                return ResponseEntity.notFound().build();
            }
            
            // Determine content type based on file extension
            String contentType = "application/octet-stream";
            String fileName = imagePath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (fileName.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (fileName.endsWith(".svg")) {
                contentType = "image/svg+xml";
            }
            
            byte[] imageBytes = Files.readAllBytes(imagePath);
            log.info("Serving image file: {} ({} bytes)", imagePath, imageBytes.length);
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .body(imageBytes);
                    
        } catch (Exception e) {
            log.error("Failed to serve image '{}' from directory '{}': {}", filename, directory, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
