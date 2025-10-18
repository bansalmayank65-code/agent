package com.amazon.agenticworkstation.action.scenario;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.amazon.agenticworkstation.action.scenario.ScenarioMergerService.ScenarioMergerResult;
import com.amazon.agenticworkstation.action.scenario.ScenarioMergerService.ScenarioStepInfo;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioStep;

/**
 * Unit tests for ScenarioMergerService.
 */
class ScenarioMergerServiceTest {

    @Test
    void testMergeTwoScenarios() {
        // Arrange
        List<String> scenarioNames = Arrays.asList(
                "UserProvisioningScenario",
                "CreateDepartmentScenario"
        );

        // Act
        ScenarioMergerResult result = ScenarioMergerService.mergeScenarios(
                scenarioNames,
                "hr_experts",
                1,
                new HashMap<>()
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getSourceScenarios().size());
        assertEquals("hr_experts", result.getEnvironment());
        assertEquals(1, result.getInterfaceNumber());
        assertTrue(result.getMergedSteps().size() > 0);
        assertTrue(result.getTotalStepsBeforeMerge() >= result.getMergedSteps().size());
        
        System.out.println(result.getSummary());
    }

    @Test
    void testMergeMultipleScenarios() {
        // Arrange
        List<String> scenarioNames = Arrays.asList(
                "UserProvisioningScenario",
                "CreateDepartmentScenario",
                "CreateJobPositionScenario",
                "PostJobOpeningScenario"
        );

        // Act
        ScenarioMergerResult result = ScenarioMergerService.mergeScenarios(
                scenarioNames,
                "hr_experts",
                1,
                new HashMap<>()
        );

        // Assert
        assertNotNull(result);
        assertEquals(4, result.getSourceScenarios().size());
        assertTrue(result.getMergedSteps().size() > 0);
        
        // Print detailed results
        System.out.println("\n" + result.getSummary());
        System.out.println("\nMerged steps:");
        for (ScenarioStep step : result.getMergedSteps()) {
            System.out.println("  - " + step.getStepId() + ": " + step.getActionName());
        }
        
        // Print duplicate information
        System.out.println("\nDuplicate analysis:");
        for (ScenarioStepInfo info : result.getStepDetails()) {
            if (info.isDuplicate()) {
                System.out.println("  - Step '" + info.getStep().getStepId() + 
                        "' (action: '" + info.getStep().getActionName() + 
                        "') appears in: " + info.getSourceScenarios());
            }
        }
    }

    @Test
    void testMergeSingleScenario() {
        // Arrange
        List<String> scenarioNames = Arrays.asList("UserProvisioningScenario");

        // Act
        ScenarioMergerResult result = ScenarioMergerService.mergeScenarios(
                scenarioNames,
                "hr_experts",
                1,
                new HashMap<>()
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getSourceScenarios().size());
        assertEquals(0, result.getDuplicatesRemoved()); // No duplicates with single scenario
        assertEquals(result.getTotalStepsBeforeMerge(), result.getMergedSteps().size());
    }

    @Test
    void testInvalidEnvironment() {
        // Arrange
        List<String> scenarioNames = Arrays.asList("UserProvisioningScenario");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            ScenarioMergerService.mergeScenarios(
                    scenarioNames,
                    "invalid_env",
                    1,
                    new HashMap<>()
            );
        });
    }

    @Test
    void testInvalidScenarioName() {
        // Arrange
        List<String> scenarioNames = Arrays.asList("NonExistentScenario");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            ScenarioMergerService.mergeScenarios(
                    scenarioNames,
                    "hr_experts",
                    1,
                    new HashMap<>()
            );
        });
    }

    @Test
    void testEmptyScenarioList() {
        // Arrange
        List<String> scenarioNames = Arrays.asList();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            ScenarioMergerService.mergeScenarios(
                    scenarioNames,
                    "hr_experts",
                    1,
                    new HashMap<>()
            );
        });
    }

    @Test
    void testNullScenarioList() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            ScenarioMergerService.mergeScenarios(
                    null,
                    "hr_experts",
                    1,
                    new HashMap<>()
            );
        });
    }

    @Test
    void testInvalidInterfaceNumber() {
        // Arrange
        List<String> scenarioNames = Arrays.asList("UserProvisioningScenario");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            ScenarioMergerService.mergeScenarios(
                    scenarioNames,
                    "hr_experts",
                    0, // Invalid - should be 1-5
                    new HashMap<>()
            );
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ScenarioMergerService.mergeScenarios(
                    scenarioNames,
                    "hr_experts",
                    6, // Invalid - should be 1-5
                    new HashMap<>()
            );
        });
    }

    @Test
    void testMergeAllHRScenarios() {
        // Arrange - all 29 HR scenarios
        List<String> scenarioNames = Arrays.asList(
                "UserProvisioningScenario",
                "CreateDepartmentScenario",
                "CreateJobPositionScenario",
                "PostJobOpeningScenario",
                "ManageJobPositionSkillsScenario",
                "ManageSkillsCreateScenario",
                "CloseJobOpeningScenario",
                "AddCandidateRecordScenario",
                "CreateJobApplicationScenario",
                "ManageApplicationStageScenario",
                "ScheduleInterviewScenario",
                "RecordInterviewOutcomeScenario",
                "EmployeeOnboardingScenario",
                "UpdateEmployeeProfileScenario",
                "EmployeeOffboardingScenario",
                "TimesheetSubmissionScenario",
                "TimesheetApprovalScenario",
                "ProcessPayrollRunScenario",
                "PayrollDeductionManagementScenario",
                "PayrollCorrectionScenario",
                "CreateBenefitsPlanScenario",
                "EmployeeBenefitsEnrollmentScenario",
                "CreatePerformanceReviewScenario",
                "CreateTrainingProgramScenario",
                "EmployeeTrainingEnrollmentScenario",
                "DocumentUploadScenario",
                "LeaveRequestProcessingScenario",
                "CreateExpenseReimbursementScenario",
                "ProcessExpenseReimbursementScenario"
        );

        // Act
        ScenarioMergerResult result = ScenarioMergerService.mergeScenarios(
                scenarioNames,
                "hr_experts",
                1,
                new HashMap<>()
        );

        // Assert
        assertNotNull(result);
        assertEquals(29, result.getSourceScenarios().size());
        assertTrue(result.getMergedSteps().size() > 0);
        
        System.out.println("\n=== ALL HR SCENARIOS MERGE ===");
        System.out.println(result.getSummary());
        System.out.println("\nTotal unique actions after deduplication: " + result.getMergedSteps().size());
        
        // Count discover_user steps (should be 1 after deduplication)
        long discoverUserCount = result.getMergedSteps().stream()
                .filter(step -> "discover_user".equals(step.getActionName()))
                .count();
        System.out.println("Discover user steps in merged result: " + discoverUserCount);
        
        // Show which steps appeared in multiple scenarios
        System.out.println("\nMost commonly duplicated steps:");
        result.getStepDetails().stream()
                .filter(ScenarioStepInfo::isDuplicate)
                .sorted((a, b) -> Integer.compare(
                        b.getSourceScenarios().size(), 
                        a.getSourceScenarios().size()))
                .limit(10)
                .forEach(info -> {
                    System.out.println(String.format("  - Action '%s' appears in %d scenarios: %s",
                            info.getStep().getActionName(),
                            info.getSourceScenarios().size(),
                            info.getSourceScenarios()));
                });
    }
}
