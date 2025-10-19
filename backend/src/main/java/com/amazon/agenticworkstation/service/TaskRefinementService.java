package com.amazon.agenticworkstation.service;

import java.util.*;

import org.springframework.stereotype.Service;

import com.amazon.agenticworkstation.dto.TaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for refining task.json files:
 * 1. Merge duplicate actions (keep first occurrence, remove duplicates)
 * 2. Move all *_audit_logs actions to the end (preserve order among them)
 * 3. Add/fix edges using EdgeGenerator
 * 4. Fix num_of_edges based on calculated edges
 */
@Service
public class TaskRefinementService {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Refines a task by performing selected refinement operations:
     * - Merge duplicate actions (if enabled)
     * - Move audit log actions to end (if enabled)
     * - Generate edges (if enabled)
     * - Update num_of_edges (if enabled)
     * 
     * @param taskData The task data to refine
     * @param options Map of operation flags (mergeDuplicates, moveAuditLogs, generateEdges, updateNumEdges)
     * @return Refined task data
     */
    public Map<String, Object> refineTask(Map<String, Object> taskData, Map<String, Object> options) {
        // Extract options with defaults (all enabled by default)
        boolean mergeDuplicates = getBooleanOption(options, "mergeDuplicates", true);
        boolean moveAuditLogs = getBooleanOption(options, "moveAuditLogs", true);
        boolean generateEdges = getBooleanOption(options, "generateEdges", true);
        boolean updateNumEdges = getBooleanOption(options, "updateNumEdges", true);
        
        // Create a copy to avoid mutating the original
        Map<String, Object> refinedTask = new LinkedHashMap<>(taskData);
        
        // Extract task object if it exists, otherwise work with root
        Map<String, Object> task = extractTaskObject(refinedTask);
        
        // Extract actions
        List<Map<String, Object>> actions = extractActions(task);
        if (actions != null && !actions.isEmpty()) {
            List<Map<String, Object>> processedActions = actions;
            
            // Step 1: Merge duplicate actions (if enabled)
            if (mergeDuplicates) {
                processedActions = mergeDuplicateActions(processedActions);
            }
            
            // Step 2: Move audit log actions to end (if enabled)
            if (moveAuditLogs) {
                processedActions = moveAuditLogsToEnd(processedActions);
            }
            
            // Update actions in task
            task.put("actions", processedActions);
            
            // Step 3: Generate edges using EdgeGenerator (if enabled)
            if (generateEdges) {
                try {
                    // Convert actions to ActionDto for EdgeGenerator
                    List<TaskDto.ActionDto> actionDtos = convertToActionDtos(processedActions);
                    List<TaskDto.EdgeDto> generatedEdges = EdgeGenerator.edgesFromActions(actionDtos);
                    
                    // Convert EdgeDto back to Map for JSON
                    List<Map<String, Object>> edgesMap = convertEdgesToMap(generatedEdges);
                    task.put("edges", edgesMap);
                    
                    // Step 4: Fix num_of_edges (if enabled)
                    if (updateNumEdges) {
                        task.put("num_edges", edgesMap.size());
                    }
                    
                } catch (Exception e) {
                    // If edge generation fails, keep existing edges or set to empty
                    System.err.println("Error generating edges: " + e.getMessage());
                    if (!task.containsKey("edges")) {
                        task.put("edges", new ArrayList<>());
                        if (updateNumEdges) {
                            task.put("num_edges", 0);
                        }
                    }
                }
            } else if (updateNumEdges) {
                // If edges generation is disabled but update num_edges is enabled,
                // update based on existing edges
                if (task.containsKey("edges")) {
                    Object edgesObj = task.get("edges");
                    if (edgesObj instanceof List) {
                        task.put("num_edges", ((List<?>) edgesObj).size());
                    }
                }
            }
        }
        
        // Put the task object back if it was nested
        if (refinedTask.containsKey("task")) {
            refinedTask.put("task", task);
        } else {
            refinedTask = task;
        }
        
        return refinedTask;
    }
    
    /**
     * Legacy method for backward compatibility - performs all refinement operations
     */
    public Map<String, Object> refineTask(Map<String, Object> taskData) {
        return refineTask(taskData, new HashMap<>());
    }
    
    /**
     * Helper method to extract boolean option with default value
     */
    private boolean getBooleanOption(Map<String, Object> options, String key, boolean defaultValue) {
        if (options == null || !options.containsKey(key)) {
            return defaultValue;
        }
        Object value = options.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    /**
     * Extract the task object from the data structure.
     * Handles both { "task": {...} } and { "actions": [...], ... } formats
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractTaskObject(Map<String, Object> data) {
        if (data.containsKey("task") && data.get("task") instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) data.get("task"));
        }
        return new LinkedHashMap<>(data);
    }
    
    /**
     * Extract actions list from task object
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractActions(Map<String, Object> task) {
        if (task.containsKey("actions") && task.get("actions") instanceof List) {
            List<?> actionsList = (List<?>) task.get("actions");
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object action : actionsList) {
                if (action instanceof Map) {
                    result.add(new LinkedHashMap<>((Map<String, Object>) action));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
    
    /**
     * Merge duplicate actions - keeps the first occurrence of each unique action.
     * Two actions are considered duplicates if they have the same name and arguments.
     */
    private List<Map<String, Object>> mergeDuplicateActions(List<Map<String, Object>> actions) {
        List<Map<String, Object>> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        
        for (Map<String, Object> action : actions) {
            String actionSignature = createActionSignature(action);
            if (!seen.contains(actionSignature)) {
                seen.add(actionSignature);
                merged.add(action);
            }
        }
        
        return merged;
    }
    
    /**
     * Create a unique signature for an action based on name and arguments
     */
    private String createActionSignature(Map<String, Object> action) {
        try {
            String name = action.getOrDefault("name", "").toString();
            Object args = action.get("arguments");
            String argsJson = objectMapper.writeValueAsString(args);
            return name + "::" + argsJson;
        } catch (Exception e) {
            return action.toString();
        }
    }
    
    /**
     * Move all audit log actions to the end while preserving their relative order.
     * Audit log actions are those with names containing "_audit_log" or "audit_trail"
     */
    private List<Map<String, Object>> moveAuditLogsToEnd(List<Map<String, Object>> actions) {
        List<Map<String, Object>> nonAuditActions = new ArrayList<>();
        List<Map<String, Object>> auditActions = new ArrayList<>();
        
        for (Map<String, Object> action : actions) {
            if (isAuditLogAction(action)) {
                auditActions.add(action);
            } else {
                nonAuditActions.add(action);
            }
        }
        
        // Combine: non-audit actions first, then audit actions
        List<Map<String, Object>> reordered = new ArrayList<>(nonAuditActions);
        reordered.addAll(auditActions);
        
        return reordered;
    }
    
    /**
     * Check if an action is an audit log action
     */
    private boolean isAuditLogAction(Map<String, Object> action) {
        String name = action.getOrDefault("name", "").toString().toLowerCase();
        return name.contains("audit_log") || 
               name.contains("audit_trail") ||
               name.contains("create_audit") ||
               name.contains("insert_audit") ||
               name.contains("manage_audit") ||
               name.contains("handle_audit") ||
               name.contains("process_audit") ||
               name.contains("administer_audit") ||
               name.contains("execute_audit");
    }
    
    /**
     * Convert map-based actions to ActionDto for EdgeGenerator
     */
    @SuppressWarnings("unchecked")
    private List<TaskDto.ActionDto> convertToActionDtos(List<Map<String, Object>> actions) {
        List<TaskDto.ActionDto> actionDtos = new ArrayList<>();
        
        for (Map<String, Object> action : actions) {
            TaskDto.ActionDto dto = new TaskDto.ActionDto();
            dto.setName((String) action.get("name"));
            
            if (action.containsKey("arguments") && action.get("arguments") instanceof Map) {
                dto.setArguments((Map<String, Object>) action.get("arguments"));
            }
            
            if (action.containsKey("output")) {
                dto.setOutput(action.get("output"));
            }
            
            actionDtos.add(dto);
        }
        
        return actionDtos;
    }
    
    /**
     * Convert EdgeDto list to Map list for JSON serialization
     */
    private List<Map<String, Object>> convertEdgesToMap(List<TaskDto.EdgeDto> edges) {
        List<Map<String, Object>> edgesMaps = new ArrayList<>();
        
        for (TaskDto.EdgeDto edge : edges) {
            Map<String, Object> edgeMap = new LinkedHashMap<>();
            edgeMap.put("from", edge.getFrom());
            edgeMap.put("to", edge.getTo());
            
            if (edge.getConnection() != null) {
                Map<String, Object> connectionMap = new LinkedHashMap<>();
                connectionMap.put("output", edge.getConnection().getOutput());
                connectionMap.put("input", edge.getConnection().getInput());
                edgeMap.put("connection", connectionMap);
            }
            
            edgesMaps.add(edgeMap);
        }
        
        return edgesMaps;
    }
}
