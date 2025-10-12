package com.amazon.agenticworkstation.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazon.agenticworkstation.dto.TaskDto;
import com.amazon.agenticworkstation.dto.TaskImportRequest;
import com.amazon.agenticworkstation.service.TaskCacheService;
import com.amazon.agenticworkstation.service.TaskService;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskCacheService taskCacheService;

    // 1. Validate instruction
    @PostMapping("/validate-instruction")
    public ResponseEntity<String> validateInstruction(@RequestBody TaskDto dto) {
        return ResponseEntity.ok(taskService.validateInstruction(dto.getTask().getInstruction()));
    }

    // 2. Generate task.json
    @PostMapping("/generate-task")
    public ResponseEntity<String> generateTask(@RequestBody TaskDto dto) {
        return ResponseEntity.ok(taskService.generateTaskJson(dto));
    }

    // 3. Validate task.json (legacy method with JSON payload)
    @PostMapping("/validate-task")
    public ResponseEntity<String> validateTask(@RequestBody String taskJson) {
        return ResponseEntity.ok(taskService.validateTaskJson(taskJson));
    }
    
    // 3b. Enhanced validate task with user_id and task_id (from cache/DB)
    @PostMapping("/validate-task-by-id")
    public ResponseEntity<String> validateTaskById(@RequestParam String userId, @RequestParam String taskId) {
        try {
            // Load task from cache (which will fetch from DB if not cached)
            taskCacheService.loadTaskIntoCache(userId, taskId);
            
            // Get the task JSON from cache
            String taskJson = taskCacheService.aggregatedJson(userId, taskId);
            
            // Validate the task
            String result = taskService.validateTaskJson(taskJson);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Validation failed: " + e.getMessage());
        }
    }
    
    // Load task into cache endpoint
    @PostMapping("/load-task-cache")
    public ResponseEntity<Map<String, Object>> loadTaskIntoCache(@RequestParam String userId, @RequestParam String taskId) {
        try {
            taskCacheService.loadTaskIntoCache(userId, taskId);
            
            // Set current context for legacy compatibility
            taskCacheService.setCurrentContext(userId, taskId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Task loaded into cache successfully",
                "userId", userId,
                "taskId", taskId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to load task: " + e.getMessage(),
                "userId", userId,
                "taskId", taskId
            ));
        }
    }
    
    // Get cached task data
    @GetMapping("/cached-task")
    public ResponseEntity<Map<String, Object>> getCachedTask(@RequestParam String userId, @RequestParam String taskId) {
        try {
            TaskDto taskDto = taskCacheService.buildAggregatedTaskDto(userId, taskId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "task", taskDto
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to get cached task: " + e.getMessage()
            ));
        }
    }
    
    // Import task from JSON content
    @PostMapping("/import-task")
    public ResponseEntity<Map<String, Object>> importTask(@RequestBody TaskImportRequest request) {
        try {
            String userId = request.getUserId();
            String taskJsonContent = request.getTaskJsonContent().toString();
            
            // Generate unique task ID
            String taskId = java.util.UUID.randomUUID().toString();
            
            logger.info("Importing task {} for user {} with content size: {}", taskId, userId, taskJsonContent.length());
            
            boolean success = taskCacheService.importTaskFromJson(userId, taskId, taskJsonContent);
            
            if (success) {
                logger.info("Successfully imported task {} for user {} into database", taskId, userId);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Task imported successfully and saved to database",
                    "userId", userId,
                    "taskId", taskId
                ));
            } else {
                logger.warn("Failed to import task {} for user {} - invalid format", taskId, userId);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to import task - invalid task format",
                    "userId", userId,
                    "taskId", taskId
                ));
            }
        } catch (Exception e) {
            logger.error("Error importing task: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to import task: " + e.getMessage(),
                "userId", request != null ? request.getUserId() : "unknown"
            ));
        }
    }

    // 4. Download all files as zip
    @GetMapping("/download-all")
    public ResponseEntity<Resource> downloadAll() {
        Resource zipFile = taskService.getAllFilesAsZip();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=all_files.zip")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(zipFile);
    }
}
