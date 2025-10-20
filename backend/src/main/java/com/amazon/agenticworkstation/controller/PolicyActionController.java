package com.amazon.agenticworkstation.controller;

import com.amazon.agenticworkstation.entity.PolicyActionEntity;
import com.amazon.agenticworkstation.service.PolicyActionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Policy Actions Builder
 * Manages CRUD operations for policy actions
 */
@RestController
@RequestMapping("/api/policy-actions")
public class PolicyActionController {
    
    private static final Logger logger = LoggerFactory.getLogger(PolicyActionController.class);
    
    @Autowired
    private PolicyActionService policyActionService;
    
    /**
     * Create a new policy action
     * POST /api/policy-actions/create
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPolicyAction(@RequestBody PolicyActionEntity policyAction) {
        try {
            logger.info("Creating policy action for env: {}, interface: {}", 
                       policyAction.getEnvName(), policyAction.getInterfaceNum());
            
            PolicyActionEntity created = policyActionService.createPolicyAction(policyAction);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy action created successfully");
            response.put("data", created);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating policy action", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create policy action: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Update an existing policy action
     * PUT /api/policy-actions/update/{id}
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updatePolicyAction(
            @PathVariable Long id, 
            @RequestBody PolicyActionEntity policyAction) {
        try {
            logger.info("Updating policy action with ID: {}", id);
            
            PolicyActionEntity updated = policyActionService.updatePolicyAction(id, policyAction);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy action updated successfully");
            response.put("data", updated);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Policy action not found: {}", id);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error updating policy action", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update policy action: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Delete a policy action
     * DELETE /api/policy-actions/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deletePolicyAction(@PathVariable Long id) {
        try {
            logger.info("Deleting policy action with ID: {}", id);
            
            policyActionService.deletePolicyAction(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Policy action deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Policy action not found: {}", id);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            logger.error("Error deleting policy action", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete policy action: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get policy action by ID
     * GET /api/policy-actions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPolicyActionById(@PathVariable Long id) {
        try {
            PolicyActionEntity policyAction = policyActionService.getPolicyActionById(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", policyAction);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get all policy actions
     * GET /api/policy-actions/list
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getAllPolicyActions() {
        try {
            List<PolicyActionEntity> policyActions = policyActionService.getAllPolicyActions();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", policyActions);
            response.put("count", policyActions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching all policy actions", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch policy actions: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get policy actions with filters
     * GET /api/policy-actions/filter
     * Query params: envName (required), interfaceNum (required), policyCat1 (optional), policyCat2 (optional)
     */
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> getPolicyActionsWithFilters(
            @RequestParam String envName,
            @RequestParam Integer interfaceNum,
            @RequestParam(required = false) String policyCat1,
            @RequestParam(required = false) String policyCat2) {
        try {
            logger.info("Filtering policy actions - env: {}, interface: {}, cat1: {}, cat2: {}", 
                       envName, interfaceNum, policyCat1, policyCat2);
            
            List<PolicyActionEntity> policyActions = policyActionService.getPolicyActionsWithFilters(
                    envName, interfaceNum, policyCat1, policyCat2);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", policyActions);
            response.put("count", policyActions.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error filtering policy actions", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to filter policy actions: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get distinct environment names
     * GET /api/policy-actions/distinct/env-names
     */
    @GetMapping("/distinct/env-names")
    public ResponseEntity<Map<String, Object>> getDistinctEnvNames() {
        try {
            List<String> envNames = policyActionService.getDistinctEnvNames();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", envNames);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching distinct env names", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch environment names: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get distinct interface numbers for an environment
     * GET /api/policy-actions/distinct/interface-nums
     * Query param: envName (required)
     */
    @GetMapping("/distinct/interface-nums")
    public ResponseEntity<Map<String, Object>> getDistinctInterfaceNums(@RequestParam String envName) {
        try {
            List<Integer> interfaceNums = policyActionService.getDistinctInterfaceNums(envName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", interfaceNums);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching distinct interface nums", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch interface numbers: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get distinct policy_cat1 values
     * GET /api/policy-actions/distinct/policy-cat1
     * Query params: envName (required), interfaceNum (required)
     */
    @GetMapping("/distinct/policy-cat1")
    public ResponseEntity<Map<String, Object>> getDistinctPolicyCat1(
            @RequestParam String envName,
            @RequestParam Integer interfaceNum) {
        try {
            List<String> policyCat1Values = policyActionService.getDistinctPolicyCat1(envName, interfaceNum);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", policyCat1Values);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching distinct policy cat1", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch policy category 1 values: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get distinct policy_cat2 values
     * GET /api/policy-actions/distinct/policy-cat2
     * Query params: envName (required), interfaceNum (required), policyCat1 (required)
     */
    @GetMapping("/distinct/policy-cat2")
    public ResponseEntity<Map<String, Object>> getDistinctPolicyCat2(
            @RequestParam String envName,
            @RequestParam Integer interfaceNum,
            @RequestParam String policyCat1) {
        try {
            List<String> policyCat2Values = policyActionService.getDistinctPolicyCat2(
                    envName, interfaceNum, policyCat1);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", policyCat2Values);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching distinct policy cat2", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch policy category 2 values: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
