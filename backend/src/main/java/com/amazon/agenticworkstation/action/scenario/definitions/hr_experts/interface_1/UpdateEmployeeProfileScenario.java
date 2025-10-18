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
 * Update Employee Profile Scenario.
 * 
 * This scenario provides a complete workflow for updating employee profiles.
 * 
 * Workflow: 1. Discover employee 2. Update employee record 3. Create audit log
 * entry
 * 
 * Category: Employee Management Environment: hr_experts Interface: 1
 */
public final class UpdateEmployeeProfileScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(UpdateEmployeeProfileScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public UpdateEmployeeProfileScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public UpdateEmployeeProfileScenario(Map<String, Object> parameters) {
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
		String employeeId = (String) parameters.get("employee_id");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("employee_id_filter",
				String.format("{\"employee_id\":\"%s\",\"employment_status\":\"active\"}", employeeId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building update_employee_profile scenario configuration");

		return ScenarioConfig.builder().scenarioName("update_employee_profile")
				.description("Complete workflow for updating employee profiles").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_discover_employee")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "employee_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employees"))
								.description("Discover employee").build(),

						ScenarioStep.builder().stepId("step3_update_employee").actionName("manage_employee")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("position_id", "position_id"))
								.addInputMapping(
										InputMapping.fromScenarioInput("employment_status", "employment_status"))
								.addInputMapping(InputMapping.fromScenarioInput("manager_id", "manager_id"))
								.addInputMapping(InputMapping.fromScenarioInput("date_of_birth", "date_of_birth"))
								.addInputMapping(InputMapping.fromScenarioInput("address", "address"))
								.addInputMapping(InputMapping.fromScenarioInput("hourly_rate", "hourly_rate"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update employee record").build(),

						ScenarioStep.builder().stepId("step4_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_employee", "employee_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.addInputMapping(InputMapping.withStaticValue("operation", "update"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "employees"))
								.description("Create audit log for employee update").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("employee_id").type("string").description("ID of the employee")
						.required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("position_id").type("string")
						.description("ID of the job position").required(false).example("12345").build(),

				ScenarioInputDefinition.builder().name("employment_status").type("string")
						.description("Employment status (active, inactive, terminated)").required(false)
						.example("active").build(),

				ScenarioInputDefinition.builder().name("manager_id").type("string").description("ID of the manager")
						.required(false).example("67890").build(),

				ScenarioInputDefinition.builder().name("date_of_birth").type("date").description("Date of birth")
						.required(false).example("1990-05-15").build(),

				ScenarioInputDefinition.builder().name("address").type("string").description("Employee address")
						.required(false).example("456 Oak St, City, State").build(),

				ScenarioInputDefinition.builder().name("hourly_rate").type("number").description("Hourly rate")
						.required(false).example("70").build());
	}
}
