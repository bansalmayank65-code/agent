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
 * Employee Offboarding Scenario.
 * 
 * This scenario provides a complete workflow for employee offboarding.
 * 
 * Workflow: 1. Check approval for offboarding 2. Discover employee 3. Check
 * pending payroll, benefits, and training 4. Update employee status 5. Update
 * user account status 6. Terminate benefits and training 7. Create audit log
 * entry
 * 
 * Category: Employee Management Environment: hr_experts Interface: 1
 */
public final class EmployeeOffboardingScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(EmployeeOffboardingScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public EmployeeOffboardingScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public EmployeeOffboardingScenario(Map<String, Object> parameters) {
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
		scenarioInputs.put("payroll_filter",
				String.format("{\"employee_id\":\"%s\",\"status\":\"pending\"}", employeeId));
		scenarioInputs.put("benefits_filter",
				String.format("{\"employee_id\":\"%s\",\"status\":\"active\"}", employeeId));
		scenarioInputs.put("training_filter",
				String.format("{\"employee_id\":\"%s\",\"status\":\"enrolled\"}", employeeId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building employee_offboarding scenario configuration");

		return ScenarioConfig.builder().scenarioName("employee_offboarding")
				.description("Complete workflow for employee offboarding").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "employee_offboarding"))
								.description("Check approval for offboarding").build(),

						ScenarioStep.builder().stepId("step3_discover_employee")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "employee_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employees"))
								.description("Discover employee").build(),

						ScenarioStep.builder().stepId("step4_check_payroll").actionName("discover_payroll_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "payroll_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "payroll_records"))
								.description("Check pending payroll").build(),

						ScenarioStep.builder().stepId("step5_check_benefits").actionName("discover_benefits_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "benefits_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employee_benefits"))
								.description("Check active benefits").build(),

						ScenarioStep.builder().stepId("step6_check_training").actionName("discover_training_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "training_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employee_training"))
								.description("Check incomplete training").build(),

						ScenarioStep.builder().stepId("step7_update_employee_status").actionName("manage_employee")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.withStaticValue("employment_status", "terminated"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update employee status").build(),

						ScenarioStep.builder().stepId("step8_update_user_status").actionName("manage_user")
								.addInputMapping(InputMapping.fromPreviousActionOutput("user_id",
										"discover_user_employee_entities", "results[0].user_id"))
								.addInputMapping(InputMapping.withStaticValue("status", "inactive"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update user account status").build(),

						ScenarioStep.builder().stepId("step9_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_employee", "employee_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.addInputMapping(InputMapping.withStaticValue("operation", "offboard"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "employees"))
								.description("Create audit log for offboarding").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.manager@company.com").build(),

				ScenarioInputDefinition.builder().name("employee_id").type("string")
						.description("ID of the employee to offboard").required(true).example("54321").build());
	}
}
