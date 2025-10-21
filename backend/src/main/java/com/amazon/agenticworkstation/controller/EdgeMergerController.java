package com.amazon.agenticworkstation.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazon.agenticworkstation.dto.TaskDto;
import com.amazon.agenticworkstation.service.EdgeMergeService;

/**
 * Controller for merging duplicate edges in task.json
 * Follows the same coding guidelines as HR Expert Interface Changer
 * Uses side-by-side JSON viewer pattern
 */
@RestController
@RequestMapping("/api/edges")
public class EdgeMergerController {
    
    private static final Logger logger = LoggerFactory.getLogger(EdgeMergerController.class);
    
    @Autowired
    private EdgeMergeService edgeMergeService;
    
    /**
     * Merge duplicate edges in a task.json
     * This endpoint takes a full task JSON and returns it with merged edges
     * 
     * @param taskDto The full task DTO
     * @return ResponseEntity containing the task with merged edges
     */
    @PostMapping("/merge-duplicate-edges")
    public ResponseEntity<Map<String, Object>> mergeDuplicateEdges(@RequestBody TaskDto taskDto) {
        try {
            logger.info("Received request to merge duplicate edges");
            
            if (taskDto == null || taskDto.getTask() == null || taskDto.getTask().getEdges() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid task: task or edges are null"
                ));
            }
            
            List<TaskDto.EdgeDto> originalEdges = taskDto.getTask().getEdges();
            logger.info("Original edge count: {}", originalEdges.size());
            
            // Perform the merge using EdgeMergeService
            List<TaskDto.EdgeDto> mergedEdges = edgeMergeService.mergeAndDeduplicateEdges(originalEdges);
            logger.info("Final edge count after merge: {}", mergedEdges.size());
            
            // Calculate statistics
            int duplicatesRemoved = originalEdges.size() - mergedEdges.size();
            
            // Update the task with merged edges
            taskDto.getTask().setEdges(mergedEdges);
            
            // Update num_edges if present
            if (taskDto.getTask().getNumEdges() != null) {
                taskDto.getTask().setNumEdges(mergedEdges.size());
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Successfully merged edges. Original: %d, Final: %d (%d removed)", 
                    originalEdges.size(), mergedEdges.size(), duplicatesRemoved),
                "task", taskDto,
                "statistics", Map.of(
                    "original_edges_count", originalEdges.size(),
                    "merged_edges_count", mergedEdges.size(),
                    "duplicates_removed", duplicatesRemoved
                )
            ));
            
        } catch (Exception e) {
            logger.error("Error merging duplicate edges", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error merging edges: " + e.getMessage()
            ));
        }
    }
}
