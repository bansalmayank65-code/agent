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
 * Employee Onboarding Scenario.
 * 
 * This scenario provides a complete workflow for employee onboarding.
 * 
 * Workflow: 1. Check approval for onboarding 2. Discover user account 3.
 * Discover job position 4. Create employee record 5. Update user account status
 * 6. Create audit log entry
 * 
 * Category: Employee Management Environment: hr_experts Interface: 1
 */
public final class EmployeeOnboardingScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(EmployeeOnboardingScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public EmployeeOnboardingScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public EmployeeOnboardingScenario(Map<String, Object> parameters) {
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
		String userEmail = (String) parameters.get("user_email");
		String positionId = (String) parameters.get("position_id");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("user_email_filter", String.format("{\"email\":\"%s\"}", userEmail));
		scenarioInputs.put("position_id_filter", String.format("{\"position_id\":\"%s\"}", positionId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building employee_onboarding scenario configuration");

		return ScenarioConfig.builder().scenarioName("employee_onboarding")
				.description("Complete workflow for employee onboarding").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "employee_onboarding"))
								.description("Check approval for onboarding").build(),

						ScenarioStep.builder().stepId("step3_discover_user")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "user_email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover user account").build(),

						ScenarioStep.builder().stepId("step4_discover_position").actionName("discover_job_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "position_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "positions"))
								.description("Discover job position").build(),

						ScenarioStep.builder().stepId("step5_manage_employee").actionName("manage_employee")
								.addInputMapping(InputMapping.fromPreviousActionOutput("user_id",
										"discover_user_employee_entities", "results[0].user_id"))
								.addInputMapping(InputMapping.fromScenarioInput("position_id", "position_id"))
								.addInputMapping(InputMapping.fromScenarioInput("hire_date", "hire_date"))
								.addInputMapping(InputMapping.fromScenarioInput("manager_id", "manager_id"))
								.addInputMapping(InputMapping.fromScenarioInput("date_of_birth", "date_of_birth"))
								.addInputMapping(InputMapping.fromScenarioInput("address", "address"))
								.addInputMapping(InputMapping.fromScenarioInput("hourly_rate", "hourly_rate"))
								.addInputMapping(InputMapping.withStaticValue("employment_status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create employee record").build(),

						ScenarioStep.builder().stepId("step6_update_user_status").actionName("manage_user")
								.addInputMapping(InputMapping.fromPreviousActionOutput("user_id",
										"discover_user_employee_entities", "results[0].user_id"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update user account status").build(),

						ScenarioStep.builder().stepId("step7_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_employee", "employee_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "onboard"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "employees"))
								.description("Create audit log for onboarding").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.manager@company.com").build(),

				ScenarioInputDefinition.builder().name("user_email").type("email")
						.description("Email of the user to be onboarded").required(true)
						.example("new.employee@company.com").build(),

				ScenarioInputDefinition.builder().name("position_id").type("string")
						.description("ID of the job position").required(true).example("12345").build(),

				ScenarioInputDefinition.builder().name("hire_date").type("date").description("Date of hire")
						.required(true).example("2025-10-20").build(),

				ScenarioInputDefinition.builder().name("manager_id").type("string").description("ID of the manager")
						.required(false).example("67890").build(),

				ScenarioInputDefinition.builder().name("date_of_birth").type("date").description("Date of birth")
						.required(false).example("1990-05-15").build(),

				ScenarioInputDefinition.builder().name("address").type("string").description("Employee address")
						.required(false).example("456 Oak St, City, State").build(),

				ScenarioInputDefinition.builder().name("hourly_rate").type("number").description("Hourly rate")
						.required(false).example("65").build());
	}
}
