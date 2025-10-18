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
 * Close Job Opening Scenario.
 * 
 * This scenario provides a complete workflow for closing job openings.
 * 
 * Workflow: 1. Discover job position 2. Update job position status to closed 3.
 * Create audit log entry
 * 
 * Category: Job Position Management Environment: hr_experts Interface: 1
 */
public final class CloseJobOpeningScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(CloseJobOpeningScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public CloseJobOpeningScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public CloseJobOpeningScenario(Map<String, Object> parameters) {
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
		String positionId = (String) parameters.get("position_id");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("position_id_filter",
				String.format("{\"position_id\":\"%s\",\"status\":\"open\"}", positionId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building close_job_opening scenario configuration");

		return ScenarioConfig.builder().scenarioName("close_job_opening")
				.description("Complete workflow for closing job openings").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_discover_position").actionName("discover_job_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "position_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "positions"))
								.description("Discover job position").build(),

						ScenarioStep.builder().stepId("step3_update_position_status").actionName("manage_job_position")
								.addInputMapping(InputMapping.fromScenarioInput("position_id", "position_id"))
								.addInputMapping(InputMapping.withStaticValue("status", "closed"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update job position status to closed").build(),

						ScenarioStep.builder().stepId("step4_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_job_position", "position_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.addInputMapping(InputMapping.withStaticValue("operation", "close"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "job_positions"))
								.description("Create audit log for job closing").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(ScenarioInputDefinition.builder().name("position_id").type("string")
				.description("ID of the job position to close").required(true).example("12345").build());
	}
}
