package com.amazon.agenticworkstation.service;

import com.amazon.agenticworkstation.entity.PolicyActionEntity;
import com.amazon.agenticworkstation.repository.PolicyActionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing policy actions
 */
@Service
@Transactional
public class PolicyActionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PolicyActionService.class);
    
    @Autowired
    private PolicyActionRepository policyActionRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Validate actions JSON format according to ActionDto schema
     * Actions JSON should be an array of action objects
     * Each action must have:
     * - name (String, required)
     * - arguments (Object, required - can be Map or any JSON object)
     * - output (Object, optional - can be any type)
     */
    private void validateActionsJson(String actionsJson) {
        if (actionsJson == null || actionsJson.trim().isEmpty()) {
            throw new IllegalArgumentException("Actions JSON cannot be empty");
        }
        
        try {
            // Parse as List of Maps
            List<Map<String, Object>> actions = objectMapper.readValue(
                actionsJson, 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            if (actions.isEmpty()) {
                throw new IllegalArgumentException("Actions JSON must contain at least one action");
            }
            
            // Validate each action
            for (int i = 0; i < actions.size(); i++) {
                Map<String, Object> action = actions.get(i);
                
                // Check required field: name
                if (!action.containsKey("name") || action.get("name") == null) {
                    throw new IllegalArgumentException(
                        String.format("Action at index %d is missing required field 'name'", i)
                    );
                }
                
                if (!(action.get("name") instanceof String)) {
                    throw new IllegalArgumentException(
                        String.format("Action at index %d has invalid 'name' field - must be a string", i)
                    );
                }
                
                String name = (String) action.get("name");
                if (name.trim().isEmpty()) {
                    throw new IllegalArgumentException(
                        String.format("Action at index %d has empty 'name' field", i)
                    );
                }
                
                // Check required field: arguments
                if (!action.containsKey("arguments") || action.get("arguments") == null) {
                    throw new IllegalArgumentException(
                        String.format("Action at index %d is missing required field 'arguments'", i)
                    );
                }
                
                // Arguments must be an object (Map in Java)
                if (!(action.get("arguments") instanceof Map)) {
                    throw new IllegalArgumentException(
                        String.format("Action at index %d has invalid 'arguments' field - must be an object", i)
                    );
                }
                
                // Output field is optional, no validation needed if present
                // It can be any type: List, Map, String, Number, Boolean, etc.
            }
            
            logger.info("Actions JSON validation successful - {} actions validated", actions.size());
            
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (Exception e) {
            // Simplify error message for JSON parsing errors
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Cannot deserialize")) {
                throw new IllegalArgumentException(
                    "Invalid Actions JSON format. Must be an array of action objects. Each action needs 'name' (string) and 'arguments' (object) fields."
                );
            }
            throw new IllegalArgumentException(
                "Invalid Actions JSON format. Expected an array like: [{\"name\": \"action_name\", \"arguments\": {...}}]"
            );
        }
    }
    
    /**
     * Create a new policy action
     */
    public PolicyActionEntity createPolicyAction(PolicyActionEntity policyAction) {
        logger.info("Creating new policy action for env: {}, interface: {}", 
                    policyAction.getEnvName(), policyAction.getInterfaceNum());
        
        // Check for duplicate combination
        boolean exists = policyActionRepository.existsByEnvNameAndInterfaceNumAndPolicyCat1AndPolicyCat2(
                policyAction.getEnvName(),
                policyAction.getInterfaceNum(),
                policyAction.getPolicyCat1(),
                policyAction.getPolicyCat2()
        );
        
        if (exists) {
            throw new IllegalArgumentException(
                String.format("A policy action already exists for the combination: env_name='%s', interface_num=%d, policy_cat1='%s', policy_cat2='%s'",
                    policyAction.getEnvName(), policyAction.getInterfaceNum(), 
                    policyAction.getPolicyCat1(), policyAction.getPolicyCat2())
            );
        }
        
        // Validate actions JSON format
        validateActionsJson(policyAction.getActionsJson());
        
        PolicyActionEntity saved = policyActionRepository.save(policyAction);
        logger.info("Policy action created with ID: {}", saved.getPolicyActionId());
        return saved;
    }
    
    /**
     * Update an existing policy action
     */
    public PolicyActionEntity updatePolicyAction(Long id, PolicyActionEntity updatedPolicyAction) {
        logger.info("Updating policy action with ID: {}", id);
        
        Optional<PolicyActionEntity> existingOpt = policyActionRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Policy action not found with ID: " + id);
        }
        
        // Check if the new combination conflicts with another record (excluding current ID)
        List<PolicyActionEntity> duplicates = policyActionRepository.findDuplicateExcludingId(
                updatedPolicyAction.getEnvName(),
                updatedPolicyAction.getInterfaceNum(),
                updatedPolicyAction.getPolicyCat1(),
                updatedPolicyAction.getPolicyCat2(),
                id
        );
        
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("A policy action already exists for the combination: env_name='%s', interface_num=%d, policy_cat1='%s', policy_cat2='%s'",
                    updatedPolicyAction.getEnvName(), updatedPolicyAction.getInterfaceNum(), 
                    updatedPolicyAction.getPolicyCat1(), updatedPolicyAction.getPolicyCat2())
            );
        }
        
        // Validate actions JSON format
        validateActionsJson(updatedPolicyAction.getActionsJson());
        
        PolicyActionEntity existing = existingOpt.get();
        
        // Update fields
        existing.setEnvName(updatedPolicyAction.getEnvName());
        existing.setInterfaceNum(updatedPolicyAction.getInterfaceNum());
        existing.setPolicyCat1(updatedPolicyAction.getPolicyCat1());
        existing.setPolicyCat2(updatedPolicyAction.getPolicyCat2());
        existing.setPolicyDescription(updatedPolicyAction.getPolicyDescription());
        existing.setActionsJson(updatedPolicyAction.getActionsJson());
        
        PolicyActionEntity updated = policyActionRepository.save(existing);
        logger.info("Policy action updated with ID: {}", id);
        return updated;
    }
    
    /**
     * Delete a policy action
     */
    public void deletePolicyAction(Long id) {
        logger.info("Deleting policy action with ID: {}", id);
        
        if (!policyActionRepository.existsById(id)) {
            throw new IllegalArgumentException("Policy action not found with ID: " + id);
        }
        
        policyActionRepository.deleteById(id);
        logger.info("Policy action deleted with ID: {}", id);
    }
    
    /**
     * Get policy action by ID
     */
    public PolicyActionEntity getPolicyActionById(Long id) {
        return policyActionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Policy action not found with ID: " + id));
    }
    
    /**
     * Get all policy actions
     */
    public List<PolicyActionEntity> getAllPolicyActions() {
        return policyActionRepository.findAllByOrderByLastUpdatedAtDesc();
    }
    
    /**
     * Get policy actions by environment name
     */
    public List<PolicyActionEntity> getPolicyActionsByEnvName(String envName) {
        return policyActionRepository.findByEnvNameOrderByLastUpdatedAtDesc(envName);
    }
    
    /**
     * Get policy actions by environment name and interface number
     */
    public List<PolicyActionEntity> getPolicyActionsByEnvNameAndInterfaceNum(String envName, Integer interfaceNum) {
        return policyActionRepository.findByEnvNameAndInterfaceNumOrderByLastUpdatedAtDesc(envName, interfaceNum);
    }
    
    /**
     * Get policy actions with filters
     */
    public List<PolicyActionEntity> getPolicyActionsWithFilters(
            String envName, 
            Integer interfaceNum, 
            String policyCat1, 
            String policyCat2) {
        
        // Apply filters based on what's provided
        if (policyCat2 != null && !policyCat2.isEmpty()) {
            return policyActionRepository.findByEnvNameAndInterfaceNumAndPolicyCat1AndPolicyCat2OrderByLastUpdatedAtDesc(
                    envName, interfaceNum, policyCat1, policyCat2);
        } else if (policyCat1 != null && !policyCat1.isEmpty()) {
            return policyActionRepository.findByEnvNameAndInterfaceNumAndPolicyCat1OrderByLastUpdatedAtDesc(
                    envName, interfaceNum, policyCat1);
        } else {
            return policyActionRepository.findByEnvNameAndInterfaceNumOrderByLastUpdatedAtDesc(
                    envName, interfaceNum);
        }
    }
    
    /**
     * Get distinct environment names
     */
    public List<String> getDistinctEnvNames() {
        return policyActionRepository.findDistinctEnvNames();
    }
    
    /**
     * Get distinct interface numbers for an environment
     */
    public List<Integer> getDistinctInterfaceNums(String envName) {
        return policyActionRepository.findDistinctInterfaceNumsByEnvName(envName);
    }
    
    /**
     * Get distinct policy_cat1 values
     */
    public List<String> getDistinctPolicyCat1(String envName, Integer interfaceNum) {
        return policyActionRepository.findDistinctPolicyCat1ByEnvNameAndInterfaceNum(envName, interfaceNum);
    }
    
    /**
     * Get distinct policy_cat2 values
     */
    public List<String> getDistinctPolicyCat2(String envName, Integer interfaceNum, String policyCat1) {
        return policyActionRepository.findDistinctPolicyCat2ByEnvNameAndInterfaceNumAndPolicyCat1(
                envName, interfaceNum, policyCat1);
    }
    
    /**
     * Search policy actions by description
     */
    public List<PolicyActionEntity> searchByDescription(String searchTerm) {
        return policyActionRepository.searchByDescription(searchTerm);
    }
}
