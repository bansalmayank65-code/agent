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
 * Create Job Application Scenario.
 * 
 * This scenario provides a complete workflow for creating job applications.
 * 
 * Workflow: 1. Discover candidate 2. Discover job position 3. Discover
 * recruiter 4. Create job application 5. Create audit log entry
 * 
 * Category: Recruitment Management Environment: hr_experts Interface: 1
 */
public final class CreateJobApplicationScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(CreateJobApplicationScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public CreateJobApplicationScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public CreateJobApplicationScenario(Map<String, Object> parameters) {
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
		String candidateId = (String) parameters.get("candidate_id");
		String positionId = (String) parameters.get("position_id");
		String recruiterEmail = (String) parameters.get("recruiter_email");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("candidate_id_filter", String.format("{\"candidate_id\":\"%s\"}", candidateId));
		scenarioInputs.put("position_id_filter", String.format("{\"position_id\":\"%s\"}", positionId));
		scenarioInputs.put("recruiter_email_filter",
				String.format("{\"email\":\"%s\",\"role\":\"recruiter\"}", recruiterEmail));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building create_job_application scenario configuration");

		return ScenarioConfig.builder().scenarioName("create_job_application")
				.description("Complete workflow for creating job applications").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_discover_candidate")
								.actionName("discover_recruitment_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "candidate_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "candidates"))
								.description("Discover candidate").build(),

						ScenarioStep.builder().stepId("step3_discover_position").actionName("discover_job_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "position_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "positions"))
								.description("Discover job position").build(),

						ScenarioStep.builder().stepId("step4_discover_recruiter")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "recruiter_email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover recruiter").build(),

						ScenarioStep.builder().stepId("step5_manage_job_application")
								.actionName("manage_job_application")
								.addInputMapping(InputMapping.fromScenarioInput("candidate_id", "candidate_id"))
								.addInputMapping(InputMapping.fromScenarioInput("position_id", "position_id"))
								.addInputMapping(InputMapping.fromScenarioInput("application_date", "application_date"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("recruiter_id",
										"discover_user_employee_entities", "results[0].user_id"))
								.addInputMapping(InputMapping.withStaticValue("status", "submitted"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create job application").build(),

						ScenarioStep.builder().stepId("step6_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_job_application", "application_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "job_applications"))
								.description("Create audit log for application creation").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("candidate_id").type("string").description("ID of the candidate")
						.required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("position_id").type("string")
						.description("ID of the job position").required(true).example("12345").build(),

				ScenarioInputDefinition.builder().name("application_date").type("date")
						.description("Date of the application").required(true).example("2025-10-18").build(),

				ScenarioInputDefinition.builder().name("recruiter_email").type("email")
						.description("Email of the recruiter").required(true).example("recruiter@company.com").build());
	}
}
