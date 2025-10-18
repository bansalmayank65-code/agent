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
 * Schedule Interview Scenario.
 * 
 * This scenario provides a complete workflow for scheduling interviews.
 * 
 * Workflow: 1. Discover job application 2. Discover interviewer 3. Create
 * interview 4. Create audit log entry
 * 
 * Category: Recruitment Management Environment: hr_experts Interface: 1
 */
public final class ScheduleInterviewScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(ScheduleInterviewScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public ScheduleInterviewScenario(Map<String, Object> parameters) {
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
		String interviewerEmail = (String) parameters.get("interviewer_email");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("application_id_filter", String.format("{\"application_id\":\"%s\"}", applicationId));
		scenarioInputs.put("interviewer_email_filter", String.format("{\"email\":\"%s\"}", interviewerEmail));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building schedule_interview scenario configuration");

		return ScenarioConfig.builder().scenarioName("schedule_interview")
				.description("Complete workflow for scheduling interviews").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_discover_application")
								.actionName("discover_recruitment_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "application_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "applications"))
								.description("Discover job application").build(),

						ScenarioStep.builder().stepId("step3_discover_interviewer")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "interviewer_email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover interviewer").build(),

						ScenarioStep.builder().stepId("step4_manage_interview").actionName("manage_interview")
								.addInputMapping(InputMapping.fromScenarioInput("application_id", "application_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("interviewer_id",
										"discover_user_employee_entities", "results[0].user_id"))
								.addInputMapping(InputMapping.fromScenarioInput("interview_type", "interview_type"))
								.addInputMapping(InputMapping.fromScenarioInput("scheduled_date", "scheduled_date"))
								.addInputMapping(InputMapping.fromScenarioInput("duration_minutes", "duration_minutes"))
								.addInputMapping(InputMapping.withStaticValue("status", "scheduled"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create interview").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_interview", "interview_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "interviews"))
								.description("Create audit log for interview scheduling").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("application_id").type("string")
						.description("ID of the job application").required(true).example("98765").build(),

				ScenarioInputDefinition.builder().name("interviewer_email").type("email")
						.description("Email of the interviewer").required(true).example("interviewer@company.com")
						.build(),

				ScenarioInputDefinition.builder().name("interview_type").type("string")
						.description("Type of interview (phone, video, in_person, technical, behavioral)")
						.required(true).example("technical").build(),

				ScenarioInputDefinition.builder().name("scheduled_date").type("datetime")
						.description("Scheduled date and time for the interview").required(true)
						.example("2025-10-25T10:00:00").build(),

				ScenarioInputDefinition.builder().name("duration_minutes").type("number")
						.description("Duration of the interview in minutes").required(false).example("60").build());
	}
}
