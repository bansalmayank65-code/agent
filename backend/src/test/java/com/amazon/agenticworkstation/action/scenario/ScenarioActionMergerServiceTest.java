package com.amazon.agenticworkstation.action.scenario;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.amazon.agenticworkstation.action.scenario.ScenarioActionMergerService.ActionInfo;
import com.amazon.agenticworkstation.action.scenario.ScenarioActionMergerService.ActionMergerResult;
import com.amazon.agenticworkstation.action.scenario.ScenarioActionMergerService.ScenarioExecutionDetail;
import com.amazon.agenticworkstation.action.scenario.ScenarioActionMergerService.ScenarioExecutionRequest;
import com.amazon.agenticworkstation.dto.TaskDto;

/**
 * Unit tests for ScenarioActionMergerService.
 * 
 * Note: These tests require the backend environment to be properly configured
 * with Python executors and data files.
 */
class ScenarioActionMergerServiceTest {

    @Test
    void testExecuteAndMergeTwoScenarios() throws Exception {
        // Arrange
        Map<String, Object> userParams = new HashMap<>();
        userParams.put("email_filter", "test@example.com");
        userParams.put("username", "testuser");
        userParams.put("full_name", "Test User");
        userParams.put("email", "test@example.com");
        
        Map<String, Object> deptParams = new HashMap<>();
        deptParams.put("email_filter", "test@example.com");
        deptParams.put("department_name", "Engineering");
        deptParams.put("manager_email", "manager@example.com");
        
        List<ScenarioExecutionRequest> requests = Arrays.asList(
                new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, userParams),
                new ScenarioExecutionRequest("CreateDepartmentScenario", "hr_experts", 1, deptParams)
        );

        // Act
        ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getExecutionDetails().size());
        assertTrue(result.getMergedActions().size() > 0);
        assertTrue(result.getTotalActionsBeforeMerge() >= result.getMergedActions().size());
        
        // Verify all scenarios executed successfully
        for (ScenarioExecutionDetail detail : result.getExecutionDetails()) {
            assertTrue(detail.isSuccess(), 
                    "Scenario " + detail.getScenarioName() + " should execute successfully");
        }
        
        System.out.println("\n" + result.getSummary());
        System.out.println("\nMerged actions:");
        for (TaskDto.ActionDto action : result.getMergedActions()) {
            System.out.println("  - " + action.getName() + " with args: " + action.getArguments().keySet());
        }
    }

    @Test
    void testExecuteAndMergeMultipleScenarios() throws Exception {
        // Arrange
        Map<String, Object> params1 = new HashMap<>();
        params1.put("email_filter", "test@example.com");
        params1.put("username", "user1");
        params1.put("full_name", "User One");
        params1.put("email", "user1@example.com");
        
        Map<String, Object> params2 = new HashMap<>();
        params2.put("email_filter", "test@example.com");
        params2.put("department_name", "Engineering");
        params2.put("manager_email", "manager@example.com");
        
        Map<String, Object> params3 = new HashMap<>();
        params3.put("email_filter", "test@example.com");
        params3.put("position_title", "Senior Engineer");
        params3.put("department_id", "dept_001");
        params3.put("employment_type", "Full-time");
        
        List<ScenarioExecutionRequest> requests = Arrays.asList(
                new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, params1),
                new ScenarioExecutionRequest("CreateDepartmentScenario", "hr_experts", 1, params2),
                new ScenarioExecutionRequest("CreateJobPositionScenario", "hr_experts", 1, params3)
        );

        // Act
        ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getExecutionDetails().size());
        assertTrue(result.getMergedActions().size() > 0);
        
        System.out.println("\n" + result.getSummary());
        
        // Print execution details
        System.out.println("\nExecution details:");
        for (ScenarioExecutionDetail detail : result.getExecutionDetails()) {
            System.out.println(String.format("  - %s: %d actions in %d ms (success: %s)",
                    detail.getScenarioName(),
                    detail.getActionsGenerated(),
                    detail.getExecutionTimeMs(),
                    detail.isSuccess()));
        }
        
        // Print duplicate information
        System.out.println("\nDuplicate actions analysis:");
        for (ActionInfo info : result.getActionDetails()) {
            if (info.isDuplicate()) {
                System.out.println(String.format("  - Action '%s' generated by %d scenarios: %s",
                        info.getAction().getName(),
                        info.getSourceScenarios().size(),
                        info.getSourceScenarios()));
            }
        }
    }

    @Test
    void testExecuteSingleScenario() throws Exception {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("email_filter", "test@example.com");
        params.put("username", "testuser");
        params.put("full_name", "Test User");
        params.put("email", "test@example.com");
        
        List<ScenarioExecutionRequest> requests = Arrays.asList(
                new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, params)
        );

        // Act
        ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getExecutionDetails().size());
        assertEquals(0, result.getDuplicatesRemoved()); // No duplicates with single scenario
        assertEquals(result.getTotalActionsBeforeMerge(), result.getMergedActions().size());
        
        ScenarioExecutionDetail detail = result.getExecutionDetails().get(0);
        assertTrue(detail.isSuccess());
        assertEquals("UserProvisioningScenario", detail.getScenarioName());
    }

    @Test
    void testEmptyRequestList() {
        // Arrange
        List<ScenarioExecutionRequest> requests = Arrays.asList();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            ScenarioActionMergerService.executeAndMergeScenarios(requests);
        });
    }

    @Test
    void testNullRequestList() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            ScenarioActionMergerService.executeAndMergeScenarios(null);
        });
    }

    @Test
    void testDuplicateActionDetection() throws Exception {
        // Arrange - Execute same scenario twice with same parameters
        // This should generate duplicate actions
        Map<String, Object> params = new HashMap<>();
        params.put("email_filter", "test@example.com");
        params.put("username", "testuser");
        params.put("full_name", "Test User");
        params.put("email", "test@example.com");
        
        List<ScenarioExecutionRequest> requests = Arrays.asList(
                new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, params),
                new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, params)
        );

        // Act
        ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getExecutionDetails().size());
        
        // Should have duplicates since same scenario executed twice with same params
        int firstScenarioActions = result.getExecutionDetails().get(0).getActionsGenerated();
        int totalActionsBeforeMerge = result.getTotalActionsBeforeMerge();
        
        // Total should be 2x first scenario's actions
        assertEquals(firstScenarioActions * 2, totalActionsBeforeMerge);
        
        // After deduplication, should have only unique actions
        assertTrue(result.getDuplicatesRemoved() > 0, "Should have removed duplicates");
        
        System.out.println("\n=== DUPLICATE DETECTION TEST ===");
        System.out.println(result.getSummary());
        System.out.println("First scenario actions: " + firstScenarioActions);
        System.out.println("Total before merge: " + totalActionsBeforeMerge);
        System.out.println("Unique actions after merge: " + result.getMergedActions().size());
        System.out.println("Duplicates removed: " + result.getDuplicatesRemoved());
    }

    @Test
    void testScenarioExecutionRequest() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("key1", "value1");
        params.put("key2", 123);
        
        // Act
        ScenarioExecutionRequest request = new ScenarioExecutionRequest(
                "TestScenario", "test_env", 2, params
        );

        // Assert
        assertEquals("TestScenario", request.getScenarioName());
        assertEquals("test_env", request.getEnvironment());
        assertEquals(2, request.getInterfaceNumber());
        assertEquals(2, request.getParameters().size());
        assertEquals("value1", request.getParameters().get("key1"));
        assertEquals(123, request.getParameters().get("key2"));
    }

    @Test
    void testScenarioExecutionRequestWithNullParams() {
        // Act
        ScenarioExecutionRequest request = new ScenarioExecutionRequest(
                "TestScenario", "test_env", 1, null
        );

        // Assert
        assertNotNull(request.getParameters());
        assertTrue(request.getParameters().isEmpty());
    }

    @Test
    void testActionInfoTracking() throws Exception {
        // This test verifies the ActionInfo class correctly tracks source scenarios
        // We'll do this indirectly through a merge operation
        
        Map<String, Object> params = new HashMap<>();
        params.put("email_filter", "test@example.com");
        params.put("username", "testuser");
        params.put("full_name", "Test User");
        params.put("email", "test@example.com");
        
        List<ScenarioExecutionRequest> requests = Arrays.asList(
                new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, params),
                new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, params)
        );

        ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);

        // Find actions that are duplicates
        List<ActionInfo> duplicates = result.getActionDetails().stream()
                .filter(ActionInfo::isDuplicate)
                .toList();
        
        assertTrue(duplicates.size() > 0, "Should have some duplicate actions");
        
        for (ActionInfo info : duplicates) {
            assertEquals(2, info.getSourceScenarios().size(), 
                    "Duplicate actions should come from 2 scenarios");
            assertTrue(info.getSourceScenarios().contains("UserProvisioningScenario"));
        }
    }
}
