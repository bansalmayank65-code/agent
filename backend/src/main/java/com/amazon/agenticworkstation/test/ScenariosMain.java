package com.amazon.agenticworkstation.test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.amazon.agenticworkstation.action.scenario.ActionGeneratorBasedOnScenario.ScenarioExecutionResult;
import com.amazon.agenticworkstation.action.scenario.Scenarios;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioExecutionException;

/**
 * Demonstration class showing how to use the Scenarios class.
 * 
 * This class demonstrates: 1. How to register scenarios using the Scenarios
 * class 2. How to execute registered scenarios 3. How to handle scenario
 * results
 */
public class ScenariosMain {

	public static void main(String[] args) throws ScenarioExecutionException {
		String scenarioName = "ManageSkillsCreateScenario";

		// Prepare scenario inputs
		String performerEmail = "johnny.jones@protonmail.com";
		String skillName = "Cyber security";
		HashMap<String, Object> scenarioInputs = new LinkedHashMap<>();
		scenarioInputs.put("requester_email", performerEmail);
		scenarioInputs.put("skill_name", skillName);

		// Execute the scenario
		ScenarioExecutionResult result = Scenarios.executeScenario(scenarioName, "hr_experts", 1, scenarioInputs);

		System.out.println("Actions executed:");
		System.out.println("================");
		result.getActions().forEach(action -> {
			System.out.println(action);
		});

		System.out.println("\n=== Demo Completed Successfully ===");
	}
}
