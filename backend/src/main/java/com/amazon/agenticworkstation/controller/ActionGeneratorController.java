package com.amazon.agenticworkstation.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazon.agenticworkstation.action.ActionGenerator;
import com.amazon.agenticworkstation.dto.TaskDto;

/**
 * Controller for generating actions using ActionGenerator
 */
@RestController
@RequestMapping("/api/actions")
public class ActionGeneratorController {
	private static final Logger log = LoggerFactory.getLogger(ActionGeneratorController.class);

	/**
	 * Generate actions from action names and inputs
	 * 
	 * Example request body: { "actionNames": ["manage_department",
	 * "manage_employee"], "allInputs": { "action": "create", "department_name":
	 * "Engineering", "manager_id": "123", "user_id": "456", "position_id": "789",
	 * "hire_date": "2025-01-15" }, "envName": "hr_experts", "interfaceNumber": 1 }
	 */
	@PostMapping("/generate")
	public ResponseEntity<?> generateActions(@RequestBody GenerateActionsRequest request) {
		try {
			log.info("Received request to generate {} actions for env: {}, interface: {}",
					request.getActionNames().size(), request.getEnvName(), request.getInterfaceNumber());

			// Validate required parameters
			if (request.getEnvName() == null || request.getInterfaceNumber() == null) {
				Map<String, Object> errorResponse = new HashMap<>();
				errorResponse.put("success", false);
				errorResponse.put("error", "envName and interfaceNumber are required parameters");
				return ResponseEntity.badRequest().body(errorResponse);
			}

			List<TaskDto.ActionDto> actions = ActionGenerator.generateActions(request.getActionNames(), null,
					request.getAllInputs(), request.getEnvName(), request.getInterfaceNumber());

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("actions", actions);
			response.put("count", actions.size());

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("Failed to generate actions", e);

			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("error", e.getMessage());

			return ResponseEntity.badRequest().body(errorResponse);
		}
	}

	/**
	 * Request DTO for action generation
	 */
	public static class GenerateActionsRequest {
		private List<String> actionNames;
		private Map<String, String> allInputs;
		private String envName;
		private Integer interfaceNumber;

		// Getters and setters
		public List<String> getActionNames() {
			return actionNames;
		}

		public void setActionNames(List<String> actionNames) {
			this.actionNames = actionNames;
		}

		public Map<String, String> getAllInputs() {
			return allInputs;
		}

		public void setAllInputs(Map<String, String> allInputs) {
			this.allInputs = allInputs;
		}

		public String getEnvName() {
			return envName;
		}

		public void setEnvName(String envName) {
			this.envName = envName;
		}

		public Integer getInterfaceNumber() {
			return interfaceNumber;
		}

		public void setInterfaceNumber(Integer interfaceNumber) {
			this.interfaceNumber = interfaceNumber;
		}
	}
}
