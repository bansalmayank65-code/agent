package com.amazon.agenticworkstation.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.amazon.agenticworkstation.service.TaskRefinementService;

/**
 * Controller for task refinement operations.
 * Provides endpoint to refine task.json by:
 * - Merging duplicate actions
 * - Moving audit log actions to end
 * - Generating edges
 * - Fixing num_of_edges
 */
@RestController
@RequestMapping("/api/refine-task")
@CrossOrigin(origins = "*")
public class TaskRefinementController {
    
    @Autowired
    private TaskRefinementService taskRefinementService;
    
    /**
     * Refine a task.json by performing all refinement operations.
     * Expected input: { "task": { "actions": [...], ... }, "options": { "mergeDuplicates": true, ... } }
     * or { "actions": [...], "options": { ... }, ... }
     * Returns: { "success": true, "refined_task": {...}, "message": "..." }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> refineTask(@RequestBody Map<String, Object> requestData) {
        try {
            // Validate input
            if (requestData == null || requestData.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Task data is required"
                ));
            }
            
            // Extract options if provided
            Map<String, Object> options = new HashMap<>();
            if (requestData.containsKey("options") && requestData.get("options") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> opts = (Map<String, Object>) requestData.get("options");
                options = opts;
            }
            
            // Extract task data (exclude options from task data)
            Map<String, Object> taskData = new HashMap<>(requestData);
            taskData.remove("options");
            
            // Check if actions exist
            boolean hasActions = false;
            if (taskData.containsKey("actions")) {
                hasActions = true;
            } else if (taskData.containsKey("task") && taskData.get("task") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> task = (Map<String, Object>) taskData.get("task");
                hasActions = task.containsKey("actions");
            }
            
            if (!hasActions) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Task must contain 'actions' field"
                ));
            }
            
            // Perform refinement with options
            Map<String, Object> result = taskRefinementService.refineTask(taskData, options);
            
            // Build response - extract refined_task and statistics from result
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("refined_task", result.get("refined_task"));
            response.put("message", "Task refined successfully");
            
            // Add statistics if available
            if (result.containsKey("statistics")) {
                response.put("statistics", result.get("statistics"));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error refining task: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for the refinement service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "TaskRefinementController"
        ));
    }
}
