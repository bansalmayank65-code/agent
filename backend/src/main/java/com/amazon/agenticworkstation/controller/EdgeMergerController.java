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
            
            // Perform the merge using EdgeMergeService with detailed results
            EdgeMergeService.EdgeMergeResult mergeResult = edgeMergeService.mergeAndDeduplicateEdgesDetailed(originalEdges);
            List<TaskDto.EdgeDto> mergedEdges = mergeResult.getEdges();
            logger.info("Final edge count after merge: {}", mergedEdges.size());
            logger.info("Merge statistics - Exact duplicates: {}, Same from/to merged: {}, Redundant connections: {}", 
                mergeResult.getExactDuplicatesRemoved(), mergeResult.getSameFromToMerged(), mergeResult.getRedundantConnectionsRemoved());
            
            // Update the task with merged edges
            taskDto.getTask().setEdges(mergedEdges);
            
            // Update num_edges if present
            if (taskDto.getTask().getNumEdges() != null) {
                taskDto.getTask().setNumEdges(mergedEdges.size());
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Successfully merged edges. Original: %d, Final: %d (%d removed)", 
                    mergeResult.getOriginalCount(), mergeResult.getFinalCount(), mergeResult.getTotalRemoved()),
                "task", taskDto,
                "statistics", Map.of(
                    "original_edges_count", mergeResult.getOriginalCount(),
                    "merged_edges_count", mergeResult.getFinalCount(),
                    "duplicates_removed", mergeResult.getTotalRemoved(),
                    "exact_duplicates_removed", mergeResult.getExactDuplicatesRemoved(),
                    "same_from_to_merged", mergeResult.getSameFromToMerged(),
                    "redundant_connections_removed", mergeResult.getRedundantConnectionsRemoved()
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
