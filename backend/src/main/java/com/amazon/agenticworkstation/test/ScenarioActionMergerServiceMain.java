package com.amazon.agenticworkstation.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.action.scenario.ScenarioActionMergerService;
import com.amazon.agenticworkstation.action.scenario.ScenarioActionMergerService.ActionMergerResult;
import com.amazon.agenticworkstation.action.scenario.ScenarioActionMergerService.ScenarioExecutionRequest;
import com.amazon.agenticworkstation.action.scenario.Scenarios;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioMetadata;
import com.amazon.agenticworkstation.dto.TaskDto;

/**
 * Unit tests for ScenarioActionMergerService.
 * 
 * Note: These tests require the backend environment to be properly configured
 * with Python executors and data files.
 */
public class ScenarioActionMergerServiceMain {
	
	private void test() throws Exception {
		// Arrange
		Map<String, Object> userParams = new HashMap<>();
		userParams.put("requester_email", "johnny.jones@protonmail.com");
		userParams.put("first_name", "mayank");
		userParams.put("last_name", "bansal");
		userParams.put("email", "mayank@example.com");
		userParams.put("role", "hr_manager");
		userParams.put("phone_number", "1212121212");

		Map<String, Object> deptParams = new HashMap<>();
		deptParams.put("requester_email", "johnny.jones@protonmail.com");
		deptParams.put("department_name", "Admin Maintenance");
		deptParams.put("manager_email", "mayank@example.com");

		List<ScenarioExecutionRequest> requests = Arrays.asList(
				// Example: pass the created user's id from the first scenario into the second
				// The mapping key is the target parameter name in CreateDepartmentScenario
				// The mapping value is of the form "ScenarioName.stepId.nested.path" or
				// "ScenarioName.@actionName.nested.path". Array indices are supported
				// e.g. "UserProvisioningScenario.step1.results[0].user_id"
				
				//,Map.of("manager_id", "UserProvisioningScenario.@manage_user.user_id")
				new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, userParams),
				new ScenarioExecutionRequest("CreateDepartmentScenario", "hr_experts", 1, deptParams));

		// Act
		ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);

		System.out.println("\n" + result.getSummary());
		System.out.println("\nMerged actions:");
		for (TaskDto.ActionDto action : result.getMergedActions()) {
			System.out.println("  - " + action.getName() + " with args: " + action.getArguments().keySet());
		}

		System.out.println("actions:[");
		result.getMergedActions().forEach(a -> {
			System.out.println(a);
		});
		System.out.println("]");
	}

	private void testExecuteAndMergeTwoScenariosAndPassOutputs() throws Exception {
		// Arrange
		Map<String, Object> userParams = new HashMap<>();
		userParams.put("requester_email", "johnny.jones@protonmail.com");
		userParams.put("first_name", "mayank");
		userParams.put("last_name", "bansal");
		userParams.put("email", "mayank@example.com");
		userParams.put("role", "hr_manager");
		userParams.put("phone_number", "1212121212");

		Map<String, Object> deptParams = new HashMap<>();
		deptParams.put("requester_email", "johnny.jones@protonmail.com");
		deptParams.put("department_name", "Admin Maintenance");
		deptParams.put("manager_email", "mayank@example.com");

		List<ScenarioExecutionRequest> requests = Arrays.asList(
				// Example: pass the created user's id from the first scenario into the second
				// The mapping key is the target parameter name in CreateDepartmentScenario
				// The mapping value is of the form "ScenarioName.stepId.nested.path" or
				// "ScenarioName.@actionName.nested.path". Array indices are supported
				// e.g. "UserProvisioningScenario.step1.results[0].user_id"
				
				//,Map.of("manager_id", "UserProvisioningScenario.@manage_user.user_id")
				new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, userParams),
				new ScenarioExecutionRequest("CreateDepartmentScenario", "hr_experts", 1, deptParams));

		// Act
		ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);

		System.out.println("\n" + result.getSummary());
		System.out.println("\nMerged actions:");
		for (TaskDto.ActionDto action : result.getMergedActions()) {
			System.out.println("  - " + action.getName() + " with args: " + action.getArguments().keySet());
		}

		System.out.println("actions:[");
		result.getMergedActions().forEach(a -> {
			System.out.println(a);
		});
		System.out.println("]");
	}

	private void testExecuteAndMergeTwoScenarios() throws Exception {
		// Arrange
		Map<String, Object> userParams = new HashMap<>();
		userParams.put("requester_email", "johnny.jones@protonmail.com");
		userParams.put("first_name", "mayank");
		userParams.put("last_name", "bansal");
		userParams.put("email", "mayank@example.com");
		userParams.put("role", "hr_manager");
		userParams.put("phone_number", "1212121212");

		Map<String, Object> deptParams = new HashMap<>();
		deptParams.put("requester_email", "johnny.jones@protonmail.com");
		deptParams.put("department_name", "Admin Maintenance");
		deptParams.put("manager_email", "reneemosley@company.com");

		List<ScenarioExecutionRequest> requests = Arrays.asList(
				new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, userParams),
				new ScenarioExecutionRequest("CreateDepartmentScenario", "hr_experts", 1, deptParams));

		// Act
		ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);

		System.out.println("\n" + result.getSummary());
		System.out.println("\nMerged actions:");
		for (TaskDto.ActionDto action : result.getMergedActions()) {
			System.out.println("  - " + action.getName() + " with args: " + action.getArguments().keySet());
		}

		System.out.println("actions:[");
		result.getMergedActions().forEach(a -> {
			System.out.println(a);
		});
		System.out.println("]");
	}

	public static void main(String[] args) throws Exception {
		ScenarioActionMergerServiceMain tester = new ScenarioActionMergerServiceMain();
		ScenarioMetadata scenarioMetadata = Scenarios.getScenarioMetadata("UserProvisioningScenario", "hr_experts", 1);
		System.out.println(scenarioMetadata);
		ScenarioMetadata scenarioMetadata1 = Scenarios.getScenarioMetadata("CreateDepartmentScenario", "hr_experts", 1);
		System.out.println(scenarioMetadata1);
		tester.testExecuteAndMergeTwoScenariosAndPassOutputs();
	}
}
