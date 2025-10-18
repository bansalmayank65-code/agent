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
 * Manage Application Stage Scenario.
 * 
 * This scenario provides a complete workflow for managing application stages.
 * 
 * Workflow: 1. Check approval for application stage change 2. Discover job
 * application 3. Update job application status 4. Create audit log entry
 * 
 * Category: Recruitment Management Environment: hr_experts Interface: 1
 */
public final class ManageApplicationStageScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(ManageApplicationStageScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public ManageApplicationStageScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public ManageApplicationStageScenario(Map<String, Object> parameters) {
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
		String applicationId = (String) parameters.get("application_id");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("application_id_filter", String.format("{\"application_id\":\"%s\"}", applicationId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building manage_application_stage scenario configuration");

		return ScenarioConfig.builder().scenarioName("manage_application_stage")
				.description("Complete workflow for managing application stages").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "application_stage_change"))
								.description("Check approval for application stage change").build(),

						ScenarioStep.builder().stepId("step3_discover_application")
								.actionName("discover_recruitment_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "application_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "applications"))
								.description("Discover job application").build(),

						ScenarioStep.builder().stepId("step4_update_application_status")
								.actionName("manage_job_application")
								.addInputMapping(InputMapping.fromScenarioInput("application_id", "application_id"))
								.addInputMapping(InputMapping.fromScenarioInput("status", "new_status"))
								.addInputMapping(
										InputMapping.fromScenarioInput("ai_screening_score", "ai_screening_score"))
								.addInputMapping(InputMapping.fromScenarioInput("final_decision", "final_decision"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update job application status").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_job_application", "application_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.addInputMapping(InputMapping.withStaticValue("operation", "stage_change"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "job_applications"))
								.description("Create audit log for stage change").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("recruiter@company.com").build(),

				ScenarioInputDefinition.builder().name("application_id").type("string")
						.description("ID of the job application").required(true).example("98765").build(),

				ScenarioInputDefinition.builder().name("new_status").type("string")
						.description("New status (submitted, screening, interviewing, offered, hired, rejected)")
						.required(true).example("screening").build(),

				ScenarioInputDefinition.builder().name("ai_screening_score").type("number")
						.description("AI screening score percentage").required(false).example("85").build(),

				ScenarioInputDefinition.builder().name("final_decision").type("string")
						.description("Final decision (hire, reject, pending)").required(false).example("pending")
						.build());
	}
}
