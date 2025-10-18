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
 * Record Interview Outcome Scenario.
 * 
 * This scenario provides a complete workflow for recording interview outcomes.
 * 
 * Workflow: 1. Discover interview 2. Update interview with outcome 3. Update
 * job application status based on outcome 4. Create audit log entry
 * 
 * Category: Recruitment Management Environment: hr_experts Interface: 1
 */
public final class RecordInterviewOutcomeScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(RecordInterviewOutcomeScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public RecordInterviewOutcomeScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public RecordInterviewOutcomeScenario(Map<String, Object> parameters) {
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
		String interviewId = (String) parameters.get("interview_id");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("interview_id_filter", String.format("{\"interview_id\":\"%s\"}", interviewId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building record_interview_outcome scenario configuration");

		return ScenarioConfig.builder().scenarioName("record_interview_outcome")
				.description("Complete workflow for recording interview outcomes").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_discover_interview")
								.actionName("discover_recruitment_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "interview_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "interviews"))
								.description("Discover interview").build(),

						ScenarioStep.builder().stepId("step3_update_interview").actionName("manage_interview")
								.addInputMapping(InputMapping.fromScenarioInput("interview_id", "interview_id"))
								.addInputMapping(InputMapping.fromScenarioInput("overall_rating", "overall_rating"))
								.addInputMapping(InputMapping.fromScenarioInput("technical_score", "technical_score"))
								.addInputMapping(
										InputMapping.fromScenarioInput("communication_score", "communication_score"))
								.addInputMapping(
										InputMapping.fromScenarioInput("cultural_fit_score", "cultural_fit_score"))
								.addInputMapping(InputMapping.fromScenarioInput("recommendation", "recommendation"))
								.addInputMapping(InputMapping.withStaticValue("status", "completed"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update interview with outcome").build(),

						ScenarioStep.builder().stepId("step4_update_application").actionName("manage_job_application")
								.addInputMapping(InputMapping.fromPreviousActionOutput("application_id",
										"discover_recruitment_entities", "results[0].application_id"))
								.addInputMapping(InputMapping.withStaticValue("status", "interviewing"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update job application status").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_interview", "interview_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.addInputMapping(InputMapping.withStaticValue("operation", "outcome"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "interviews"))
								.description("Create audit log for interview outcome").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("interview_id").type("string").description("ID of the interview")
						.required(true).example("11223").build(),

				ScenarioInputDefinition.builder().name("overall_rating").type("number")
						.description("Overall rating (1-5)").required(false).example("4").build(),

				ScenarioInputDefinition.builder().name("technical_score").type("number")
						.description("Technical score (1-5)").required(false).example("5").build(),

				ScenarioInputDefinition.builder().name("communication_score").type("number")
						.description("Communication score (1-5)").required(false).example("4").build(),

				ScenarioInputDefinition.builder().name("cultural_fit_score").type("number")
						.description("Cultural fit score (1-5)").required(false).example("5").build(),

				ScenarioInputDefinition.builder().name("recommendation").type("string")
						.description("Recommendation (hire, reject, maybe)").required(false).example("hire").build());
	}
}
