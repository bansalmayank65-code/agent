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
 * Employee Benefits Enrollment Scenario.
 * 
 * This scenario provides a complete workflow for employee benefits enrollment.
 * 
 * Workflow: 1. Discover employee 2. Discover benefits plan 3. Check existing
 * enrollments 4. Create benefits enrollment 5. Create audit log entry
 * 
 * Category: Benefits Management Environment: hr_experts Interface: 1
 */
public final class EmployeeBenefitsEnrollmentScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(EmployeeBenefitsEnrollmentScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public EmployeeBenefitsEnrollmentScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public EmployeeBenefitsEnrollmentScenario(Map<String, Object> parameters) {
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
		String planId = (String) parameters.get("plan_id");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("employee_id_filter",
				String.format("{\"employee_id\":\"%s\",\"employment_status\":\"active\"}", employeeId));
		scenarioInputs.put("plan_id_filter", String.format("{\"plan_id\":\"%s\",\"status\":\"active\"}", planId));
		scenarioInputs.put("existing_enrollment_filter",
				String.format("{\"employee_id\":\"%s\",\"status\":\"active\"}", employeeId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building employee_benefits_enrollment scenario configuration");

		return ScenarioConfig.builder().scenarioName("employee_benefits_enrollment")
				.description("Complete workflow for employee benefits enrollment").envName("hr_experts")
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

						ScenarioStep.builder().stepId("step3_discover_plan").actionName("discover_benefits_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "plan_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "benefits_plans"))
								.description("Discover benefits plan").build(),

						ScenarioStep.builder().stepId("step4_check_enrollments")
								.actionName("discover_benefits_entities")
								.addInputMapping(
										InputMapping.fromScenarioInput("filters", "existing_enrollment_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employee_benefits"))
								.description("Check existing enrollments").build(),

						ScenarioStep.builder().stepId("step5_create_enrollment").actionName("manage_employee_benefits")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("plan_id", "plan_id"))
								.addInputMapping(InputMapping.fromScenarioInput("enrollment_date", "enrollment_date"))
								.addInputMapping(InputMapping.fromScenarioInput("coverage_level", "coverage_level"))
								.addInputMapping(InputMapping.fromScenarioInput("beneficiary_name", "beneficiary_name"))
								.addInputMapping(InputMapping.fromScenarioInput("beneficiary_relationship",
										"beneficiary_relationship"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create benefits enrollment").build(),

						ScenarioStep.builder().stepId("step6_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_employee_benefits", "enrollment_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "enroll"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "employee_benefits"))
								.description("Create audit log for benefits enrollment change").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("employee_id").type("string").description("ID of the employee")
						.required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("plan_id").type("string").description("ID of the benefits plan")
						.required(true).example("78910").build(),

				ScenarioInputDefinition.builder().name("enrollment_date").type("date").description("Date of enrollment")
						.required(true).example("2025-11-01").build(),

				ScenarioInputDefinition.builder().name("coverage_level").type("string")
						.description("Coverage level (individual, family, employee_spouse, employee_children)")
						.required(true).example("family").build(),

				ScenarioInputDefinition.builder().name("beneficiary_name").type("string")
						.description("Name of the beneficiary").required(false).example("Jane Doe").build(),

				ScenarioInputDefinition.builder().name("beneficiary_relationship").type("string")
						.description("Relationship to beneficiary").required(false).example("Spouse").build());
	}
}
