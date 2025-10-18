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
 * Create Job Position Scenario.
 * 
 * This scenario provides a complete workflow for creating job positions in the
 * HR system.
 * 
 * Workflow: 1. Check approval for job position creation 2. Discover department
 * 3. Create the job position 4. Create audit log entry
 * 
 * Category: Job Position Management Environment: hr_experts Interface: 1
 */
public final class CreateJobPositionScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(CreateJobPositionScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public CreateJobPositionScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public CreateJobPositionScenario(Map<String, Object> parameters) {
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
		String departmentName = (String) parameters.get("department_name");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("department_name_filter", String.format("{\"department_name\":\"%s\"}", departmentName));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building create_job_position scenario configuration");

		return ScenarioConfig.builder().scenarioName("create_job_position")
				.description("Complete workflow for creating job positions").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "job_position_creation"))
								.description("Check approval for job position creation").build(),

						ScenarioStep.builder().stepId("step3_discover_department")
								.actionName("discover_department_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "department_name_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "departments"))
								.description("Discover department").build(),

						ScenarioStep.builder().stepId("step4_manage_job_position").actionName("manage_job_position")
								.addInputMapping(InputMapping.fromScenarioInput("title", "title"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("department_id",
										"discover_department_entities", "results[0].department_id"))
								.addInputMapping(InputMapping.fromScenarioInput("job_level", "job_level"))
								.addInputMapping(InputMapping.fromScenarioInput("employment_type", "employment_type"))
								.addInputMapping(InputMapping.fromScenarioInput("hourly_rate_min", "hourly_rate_min"))
								.addInputMapping(InputMapping.fromScenarioInput("hourly_rate_max", "hourly_rate_max"))
								.addInputMapping(InputMapping.withStaticValue("status", "draft"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create the job position").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_job_position", "position_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "job_positions"))
								.description("Create audit log for job position creation").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.director@company.com").build(),

				ScenarioInputDefinition.builder().name("title").type("string").description("Job position title")
						.required(true).example("Senior Software Engineer").build(),

				ScenarioInputDefinition.builder().name("department_name").type("string")
						.description("Name of the department").required(true).example("Engineering").build(),

				ScenarioInputDefinition.builder().name("job_level").type("string")
						.description("Job level (entry, junior, mid, senior, lead, manager, director, executive)")
						.required(true).example("senior").build(),

				ScenarioInputDefinition.builder().name("employment_type").type("string")
						.description("Employment type (full_time, part_time, contract, intern)").required(true)
						.example("full_time").build(),

				ScenarioInputDefinition.builder().name("hourly_rate_min").type("number")
						.description("Minimum hourly rate").required(false).example("50").build(),

				ScenarioInputDefinition.builder().name("hourly_rate_max").type("number")
						.description("Maximum hourly rate").required(false).example("80").build());
	}
}
