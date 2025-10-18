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
 * Employee Training Enrollment Scenario.
 * 
 * This scenario provides a complete workflow for employee training enrollment.
 * 
 * Workflow: 1. Discover employee 2. Discover training program 3. Create
 * training enrollment 4. Create audit log entry
 * 
 * Category: Training Management Environment: hr_experts Interface: 1
 */
public final class EmployeeTrainingEnrollmentScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(EmployeeTrainingEnrollmentScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public EmployeeTrainingEnrollmentScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public EmployeeTrainingEnrollmentScenario(Map<String, Object> parameters) {
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
		String programId = (String) parameters.get("program_id");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("employee_id_filter",
				String.format("{\"employee_id\":\"%s\",\"employment_status\":\"active\"}", employeeId));
		scenarioInputs.put("program_id_filter",
				String.format("{\"program_id\":\"%s\",\"status\":\"active\"}", programId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building employee_training_enrollment scenario configuration");

		return ScenarioConfig.builder().scenarioName("employee_training_enrollment")
				.description("Complete workflow for employee training enrollment").envName("hr_experts")
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

						ScenarioStep.builder().stepId("step3_discover_program").actionName("discover_training_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "program_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "training_programs"))
								.description("Discover training program").build(),

						ScenarioStep.builder().stepId("step4_create_enrollment").actionName("manage_employee_training")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("program_id", "program_id"))
								.addInputMapping(InputMapping.fromScenarioInput("enrollment_date", "enrollment_date"))
								.addInputMapping(InputMapping.withStaticValue("status", "enrolled"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create training enrollment").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_employee_training", "training_record_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "enroll"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "employee_training"))
								.description("Create audit log for training enrollment").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("employee_id").type("string").description("ID of the employee")
						.required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("program_id").type("string")
						.description("ID of the training program").required(true).example("99887").build(),

				ScenarioInputDefinition.builder().name("enrollment_date").type("date").description("Date of enrollment")
						.required(true).example("2025-10-20").build());
	}
}
