package com.amazon.agenticworkstation.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazon.agenticworkstation.dto.TaskDto;

/**
 * Controller for merging duplicate edges in task.json
 * Follows the same coding guidelines as HR Expert Interface Changer
 * Uses side-by-side JSON viewer pattern
 */
@RestController
@RequestMapping("/api/edges")
public class EdgeMergerController {
    
    private static final Logger logger = LoggerFactory.getLogger(EdgeMergerController.class);
    
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
            
            // Perform the merge using the same logic from EdgeGenerator
            List<TaskDto.EdgeDto> mergedEdges = mergeDuplicateEdges(originalEdges);
            logger.info("Merged edge count: {}", mergedEdges.size());
            
            // Update the task with merged edges
            taskDto.getTask().setEdges(mergedEdges);
            
            // Update num_edges if present
            if (taskDto.getTask().getNumEdges() != null) {
                taskDto.getTask().setNumEdges(mergedEdges.size());
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", String.format("Successfully merged edges. Original: %d, Merged: %d", 
                    originalEdges.size(), mergedEdges.size()),
                "originalCount", originalEdges.size(),
                "mergedCount", mergedEdges.size(),
                "task", taskDto
            ));
            
        } catch (Exception e) {
            logger.error("Error merging duplicate edges", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Error merging edges: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Merge duplicate edges based on matching "from" and "to" values
     * This follows the same logic as EdgeGenerator.edgesFromActions() merging step
     * 
     * Implementation details:
     * 1. First remove exact duplicate edges (same from, to, and connections)
     * 2. Then merge edges with same from/to by combining their inputs and outputs
     * 
     * @param edges List of edges to merge
     * @return List of merged edges
     */
    private List<TaskDto.EdgeDto> mergeDuplicateEdges(List<TaskDto.EdgeDto> edges) {
        if (edges == null || edges.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Step 1: Remove exact duplicate edges
        List<TaskDto.EdgeDto> deduplicatedEdges = new ArrayList<>();
        
        for (TaskDto.EdgeDto edge : edges) {
            boolean isDuplicate = false;
            
            // Check if this edge is an exact duplicate of any previously added edge
            for (TaskDto.EdgeDto existingEdge : deduplicatedEdges) {
                if (areEdgesExactlyEqual(edge, existingEdge)) {
                    isDuplicate = true;
                    break;
                }
            }
            
            // Only add if not an exact duplicate
            if (!isDuplicate) {
                deduplicatedEdges.add(edge);
            }
        }
        
        logger.info("After deduplication: {} edges", deduplicatedEdges.size());
        
        // Step 2: Merge edges with same "from" and "to" values
        List<TaskDto.EdgeDto> mergedEdges = new ArrayList<>();
        
        for (TaskDto.EdgeDto edge : deduplicatedEdges) {
            boolean merged = false;
            
            // Check if we already have an edge with the same from and to
            for (TaskDto.EdgeDto existingEdge : mergedEdges) {
                if (java.util.Objects.equals(edge.getFrom(), existingEdge.getFrom())
                        && java.util.Objects.equals(edge.getTo(), existingEdge.getTo())) {
                    // Merge the connections
                    mergeConnections(existingEdge, edge);
                    merged = true;
                    break;
                }
            }
            
            // If not merged, add as new edge
            if (!merged) {
                mergedEdges.add(edge);
            }
        }
        
        logger.info("After merging: {} edges", mergedEdges.size());
        return mergedEdges;
    }
    
    /**
     * Check if two edges are exactly equal in all aspects
     * Copied from EdgeGenerator for consistency
     */
    private boolean areEdgesExactlyEqual(TaskDto.EdgeDto edge1, TaskDto.EdgeDto edge2) {
        if (edge1 == edge2) {
            return true;
        }
        if (edge1 == null || edge2 == null) {
            return false;
        }
        
        // Compare from and to
        if (!java.util.Objects.equals(edge1.getFrom(), edge2.getFrom())
                || !java.util.Objects.equals(edge1.getTo(), edge2.getTo())) {
            return false;
        }
        
        // Compare connections
        TaskDto.ConnectionDto conn1 = edge1.getConnection();
        TaskDto.ConnectionDto conn2 = edge2.getConnection();
        
        if (conn1 == conn2) {
            return true;
        }
        if (conn1 == null || conn2 == null) {
            return false;
        }
        
        // Compare connection output and input
        return java.util.Objects.equals(conn1.getOutput(), conn2.getOutput())
                && java.util.Objects.equals(conn1.getInput(), conn2.getInput());
    }
    
    /**
     * Merge connections from the second edge into the first edge by combining their
     * inputs and outputs while maintaining proper input-output pairing
     * Copied from EdgeGenerator for consistency
     */
    private void mergeConnections(TaskDto.EdgeDto targetEdge, TaskDto.EdgeDto sourceEdge) {
        TaskDto.ConnectionDto targetConnection = targetEdge.getConnection();
        TaskDto.ConnectionDto sourceConnection = sourceEdge.getConnection();
        
        if (targetConnection == null) {
            targetEdge.setConnection(sourceConnection);
            return;
        }
        
        if (sourceConnection == null) {
            return;
        }
        
        // Parse existing input-output pairs from target connection
        List<String> targetOutputs = parseFields(targetConnection.getOutput());
        List<String> targetInputs = parseFields(targetConnection.getInput());
        
        // Parse input-output pairs from source connection
        List<String> sourceOutputs = parseFields(sourceConnection.getOutput());
        List<String> sourceInputs = parseFields(sourceConnection.getInput());
        
        // Merge pairs while maintaining input-output correspondence
        List<String> mergedOutputs = new ArrayList<>(targetOutputs);
        List<String> mergedInputs = new ArrayList<>(targetInputs);
        
        // Add source pairs, avoiding duplicates based on input-output combination
        int sourceSize = Math.min(sourceOutputs.size(), sourceInputs.size());
        for (int i = 0; i < sourceSize; i++) {
            String sourceOutput = sourceOutputs.get(i);
            String sourceInput = sourceInputs.get(i);
            
            // Check if this input-output pair already exists
            boolean pairExists = false;
            int targetSize = Math.min(mergedOutputs.size(), mergedInputs.size());
            for (int j = 0; j < targetSize; j++) {
                if (mergedOutputs.get(j).equals(sourceOutput) && mergedInputs.get(j).equals(sourceInput)) {
                    pairExists = true;
                    break;
                }
            }
            
            // Add the pair if it doesn't exist
            if (!pairExists) {
                mergedOutputs.add(sourceOutput);
                mergedInputs.add(sourceInput);
            }
        }
        
        // Set the merged results, ensuring equal lengths
        targetConnection.setOutput(String.join(", ", mergedOutputs));
        targetConnection.setInput(String.join(", ", mergedInputs));
    }
    
    /**
     * Parse comma-separated field string into a list of individual fields
     * Copied from EdgeGenerator for consistency
     */
    private List<String> parseFields(String fieldString) {
        List<String> fields = new ArrayList<>();
        if (fieldString == null || fieldString.trim().isEmpty()) {
            return fields;
        }
        
        for (String field : fieldString.split(",")) {
            String trimmed = field.trim();
            if (!trimmed.isEmpty()) {
                fields.add(trimmed);
            }
        }
        
        return fields;
    }
}
