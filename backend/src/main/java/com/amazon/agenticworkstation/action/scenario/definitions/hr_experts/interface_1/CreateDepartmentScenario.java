package com.amazon.agenticworkstation.action.scenario.definitions.hr_experts.interface_1;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.action.scenario.BaseScenario;
import com.amazon.agenticworkstation.action.scenario.models.InputMapping;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioConfig;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioInputDefinition;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioStep;

/**
 * Create Department Scenario.
 * 
 * This scenario provides a complete workflow for creating departments in the HR
 * system.
 * 
 * Workflow: 1. Check approval for department creation 2. Discover manager
 * employee 3. Create the department 4. Create audit log entry
 * 
 * Category: Department Management Environment: hr_experts Interface: 1
 */
public final class CreateDepartmentScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(CreateDepartmentScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public CreateDepartmentScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public CreateDepartmentScenario(Map<String, Object> parameters) {
		if (parameters == null) {
			throw new IllegalArgumentException("Parameters map cannot be null");
		}

		for (ScenarioInputDefinition inputDef : requiredInputs) {
			if (inputDef.isRequired()) {
				validateRequiredParameter(parameters, inputDef.getName(), inputDef.getType());
			}
		}

		this.parameters = new LinkedHashMap<>(parameters);
	}

	@Override
	public ScenarioConfig getScenarioConfig() {
		return config;
	}

	@Override
	public List<ScenarioInputDefinition> getRequiredInputs() {
		return requiredInputs;
	}

	public Object getParameter(String paramName) {
		return parameters.get(paramName);
	}

	public Map<String, Object> getParameters() {
		return new LinkedHashMap<>(parameters);
	}

	@Override
	public Map<String, Object> getScenarioInputs() {
		Map<String, Object> scenarioInputs = new LinkedHashMap<>();
		scenarioInputs.putAll(parameters);

		String requesterEmail = (String) parameters.get("requester_email");
		String managerEmail = (String) parameters.get("manager_email");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("manager_email_filter", String.format("{\"email\":\"%s\"}", managerEmail));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building create_department scenario configuration");

		return ScenarioConfig.builder().scenarioName("create_department")
				.description("Complete workflow for creating departments").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "create_department"))
								.description("Check approval for department creation").build(),

						ScenarioStep.builder().stepId("step3_discover_manager")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "manager_email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step4_manage_department").actionName("manage_department")
								.addInputMapping(InputMapping.fromScenarioInput("department_name", "department_name"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("manager_id",
										"discover_user_employee_entities", "results[0].user_id"))
								.addInputMapping(InputMapping.fromScenarioInput("budget", "budget"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create the department").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id",
										"step1_discover_user", "results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_department", "department_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "departments"))
								.description("Create audit log for department creation").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.director@company.com").build(),

				ScenarioInputDefinition.builder().name("department_name").type("string")
						.description("Name of the department").required(true).example("Engineering").build(),

				ScenarioInputDefinition.builder().name("manager_email").type("email")
						.description("Email of the department manager (Must exist in User table)").required(true)
						.example("manager@test.com").build(),

				ScenarioInputDefinition.builder().name("budget").type("number")
						.description("Department budget (Must be a positive number)").required(false).example("500000")
						.build());
	}
}
