package com.amazon.agenticworkstation.test;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.action.ActionGenerator;
import com.amazon.agenticworkstation.dto.TaskDto;

/**
 * Standalone test class for ActionGenerator without Spring Boot. Demonstrates
 * how to use ActionGenerator to generate actions from instruction inputs.
 */
public class ActionGeneratorMain {

	private void generate() throws IOException {
		String envName = "hr_experts";
		int interfaceNumber = 1;

		List<String> actionNames = Arrays.asList("discover_user_employee_entities", "check_approval",
				"discover_job_entities", "manage_skill");

		Map<String, String> instructionInputs = new LinkedHashMap<>();
		instructionInputs.put("email", "johnny.jones@protonmail.com");
		instructionInputs.put("requester_email", "johnny.jones@protonmail.com");
		instructionInputs.put("first_name", "Johnny");
		instructionInputs.put("last_name", "Jones");

		instructionInputs.put("skill_name", "Cyber security");

		Map<String, Map<String, String>> actionInputs = new LinkedHashMap<>();
		actionNames.forEach(actionName -> {
			actionInputs.put(actionName, new LinkedHashMap<>());
		});

		actionInputs.get("discover_user_employee_entities").put("entity_type", "users");
		actionInputs.get("discover_user_employee_entities").put("filters", "{\"email\":\"johnny.jones@protonmail.com\"}");
		actionInputs.get("check_approval").put("action", "skills_management");
		actionInputs.get("discover_job_entities").put("entity_type", "skills");
		actionInputs.get("discover_job_entities").put("filters", "{\"skill_name\":\"Cyber security\"}");
		actionInputs.get("manage_skill").put("status", "active");
		actionInputs.get("manage_skill").put("action", "create");

		// Act
		List<TaskDto.ActionDto> actions = ActionGenerator.generateActions(actionNames, actionInputs, instructionInputs,
				envName, interfaceNumber);

		// Assert
		System.out.println("actions:[");
		actions.forEach(a -> {
			System.out.println(a);
		});
		System.out.println("]");
	}

	public static void main(String[] args) throws IOException {
		new ActionGeneratorMain().generate();
	}
}
