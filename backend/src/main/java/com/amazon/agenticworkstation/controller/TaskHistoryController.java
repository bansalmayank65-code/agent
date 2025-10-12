package com.amazon.agenticworkstation.controller;

import com.amazon.agenticworkstation.entity.TaskEntity;
import com.amazon.agenticworkstation.service.TaskHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for task history functionality
 */
@RestController
@RequestMapping("/api/tasks-history")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TaskHistoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskHistoryController.class);
    
    @Autowired
    private TaskHistoryService taskHistoryService;
    
    /**
     * Get all tasks accessible by the logged-in user
     * GET /api/tasks-history/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getTasksForUser(
            @PathVariable @NotBlank String userId) {
        
        logger.info("API request to get tasks for user: {}", userId);
        
        try {
            List<TaskEntity> tasks = taskHistoryService.getTasksForUser(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tasks);
            response.put("count", tasks.size());
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting tasks for user: " + userId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving tasks");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get tasks by status for a user
     * GET /api/tasks-history/user/{userId}/status/{status}
     */
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<Map<String, Object>> getTasksByStatus(
            @PathVariable @NotBlank String userId,
            @PathVariable @NotBlank String status) {
        
        logger.info("API request to get tasks for user: {} with status: {}", userId, status);
        
        try {
            List<TaskEntity> tasks = taskHistoryService.getTasksByStatus(userId, status);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tasks);
            response.put("count", tasks.size());
            response.put("userId", userId);
            response.put("status", status);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting tasks for user: {} with status: {}", userId, status, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving tasks by status");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get task statistics for a user
     * GET /api/tasks-history/user/{userId}/statistics
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<Map<String, Object>> getTaskStatistics(
            @PathVariable @NotBlank String userId) {
        
        logger.info("API request to get task statistics for user: {}", userId);
        
        try {
            Map<String, Object> statistics = taskHistoryService.getTaskStatistics(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", statistics);
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting task statistics for user: " + userId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving task statistics");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get detailed task by ID (with access control)
     * GET /api/tasks-history/user/{userId}/task/{taskId}
     */
    @GetMapping("/user/{userId}/task/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskDetails(
            @PathVariable @NotBlank String userId,
            @PathVariable @NotBlank String taskId) {
        
        logger.info("API request to get task details for user: {} and task: {}", userId, taskId);
        
        try {
            TaskEntity task = taskHistoryService.getTaskDetails(userId, taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", task);
            response.put("userId", userId);
            response.put("taskId", taskId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Access denied or task not found: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            
        } catch (Exception e) {
            logger.error("Error getting task details for user: {} and task: {}", userId, taskId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving task details");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get recent tasks for a user (last 10)
     * GET /api/tasks-history/user/{userId}/recent
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<Map<String, Object>> getRecentTasks(
            @PathVariable @NotBlank String userId) {
        
        logger.info("API request to get recent tasks for user: {}", userId);
        
        try {
            List<TaskEntity> tasks = taskHistoryService.getRecentTasks(userId, 10);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", tasks);
            response.put("count", tasks.size());
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting recent tasks for user: " + userId, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error retrieving recent tasks");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}