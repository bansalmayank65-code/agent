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
 * Manage Job Position Skills Scenario.
 * 
 * This scenario provides a complete workflow for managing skills associated
 * with job positions.
 * 
 * Workflow: 1. Check approval for position skills management 2. Discover job
 * position 3. Discover skills 4. Add or remove skill associations 5. Create
 * audit log entry
 * 
 * Category: Job Position Management Environment: hr_experts Interface: 1
 */
public final class ManageJobPositionSkillsScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(ManageJobPositionSkillsScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public ManageJobPositionSkillsScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public ManageJobPositionSkillsScenario(Map<String, Object> parameters) {
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
		scenarioInputs.put("position_id_filter", String.format("{\"position_id\":\"%s\"}", positionId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building manage_job_position_skills scenario configuration");

		return ScenarioConfig.builder().scenarioName("manage_job_position_skills")
				.description("Complete workflow for managing job position skills").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "position_skills_management"))
								.description("Check approval for position skills management").build(),

						ScenarioStep.builder().stepId("step3_discover_position").actionName("discover_job_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "position_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "positions"))
								.description("Discover job position").build(),

						ScenarioStep.builder().stepId("step4_discover_skills").actionName("discover_job_entities")
								.addInputMapping(InputMapping.withStaticValue("filters", "{}"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "skills"))
								.description("Discover skills").build(),

						ScenarioStep.builder().stepId("step5_manage_position_skills")
								.actionName("manage_job_position_skills")
								.addInputMapping(InputMapping.fromScenarioInput("position_id", "position_id"))
								.addInputMapping(InputMapping.fromScenarioInput("skill_ids", "skill_ids"))
								.addInputMapping(InputMapping.fromScenarioInput("action", "action"))
								.description("Add or remove skill associations").build(),

						ScenarioStep.builder().stepId("step6_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromScenarioInput("reference_id", "position_id"))
								.addInputMapping(InputMapping.fromScenarioInput("action", "action"))
								.addInputMapping(InputMapping.withStaticValue("operation", "manage_skills"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "job_position_skills"))
								.description("Create audit log for position skills management").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.director@company.com").build(),

				ScenarioInputDefinition.builder().name("position_id").type("string")
						.description("ID of the job position").required(true).example("12345").build(),

				ScenarioInputDefinition.builder().name("skill_ids").type("array")
						.description("Array of skill IDs to add or remove").required(true)
						.example("[\"101\", \"102\", \"103\"]").build(),

				ScenarioInputDefinition.builder().name("action").type("string")
						.description("Action to perform: add or remove").required(true).example("add").build());
	}
}
