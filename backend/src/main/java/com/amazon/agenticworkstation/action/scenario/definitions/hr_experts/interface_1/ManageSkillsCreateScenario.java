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
 * Manage Skills Scenario.
 * 
 * This scenario provides a complete workflow for managing employee skills in
 * the HR system.
 * 
 * Workflow: 1. Discover user/employee entities by email 2. Check approval for
 * skills management 3. Discover job entities (skills) 4. Manage skill
 * (create/update) 5. Create audit log entry
 * 
 * Category: Skills Management Environment: hr_experts Interface: 1
 */
public final class ManageSkillsCreateScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(ManageSkillsCreateScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	// Instance fields - store all parameters
	private final Map<String, Object> parameters;

	public ManageSkillsCreateScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	/**
	 * Create a new ManageSkillsScenario instance with required parameters.
	 * 
	 * @param parameters Map containing required parameters (dynamically validated
	 *                   based on getRequiredInputs())
	 * @throws IllegalArgumentException if any required parameter is missing or
	 *                                  invalid
	 */
	public ManageSkillsCreateScenario(Map<String, Object> parameters) {
		// Validate parameters map
		if (parameters == null) {
			throw new IllegalArgumentException("Parameters map cannot be null");
		}

		// Dynamically validate required parameters based on input definitions
		for (ScenarioInputDefinition inputDef : requiredInputs) {
			if (inputDef.isRequired()) {
				validateRequiredParameter(parameters, inputDef.getName(), inputDef.getType());
			}
		}

		// Store immutable copy of parameters
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

	/**
	 * Get a specific parameter value.
	 * 
	 * @param paramName The parameter name
	 * @return Parameter value, or null if not found
	 */
	public Object getParameter(String paramName) {
		return parameters.get(paramName);
	}

	/**
	 * Get all parameters.
	 * 
	 * @return Immutable copy of all parameters
	 */
	public Map<String, Object> getParameters() {
		return new LinkedHashMap<>(parameters);
	}

	/**
	 * Get all scenario inputs with generated filters.
	 * 
	 * This method prepares the complete scenario inputs map including filter
	 * patterns needed by the scenario steps.
	 * 
	 * @return Complete scenario inputs with generated filters
	 */
	@Override
	public Map<String, Object> getScenarioInputs() {
		Map<String, Object> scenarioInputs = new LinkedHashMap<>();

		// Add all direct parameters
		scenarioInputs.putAll(parameters);

		// Generate filter patterns
		String requesterEmail = (String) parameters.get("requester_email");
		String skillName = (String) parameters.get("skill_name");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("skill_name_filter", String.format("{\"skill_name\":\"%s\"}", skillName));

		return scenarioInputs;
	}

	/**
	 * Build the scenario configuration.
	 */
	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building manage_skills scenario configuration");

		return ScenarioConfig.builder().scenarioName("manage_skills")
				.description("Complete workflow for managing employee skills").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(
						// Step 1: Discover user/employee entities
						ScenarioStep.builder().stepId("step1_discover_user")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover user/employee entities by email").build(),

						// Step 2: Check approval for skills management
						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "skills_management"))
								.description("Check approval for skills management action").build(),

						// Step 3: Discover job entities (skills)
						ScenarioStep.builder().stepId("step3_discover_skills").actionName("discover_job_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "skill_name_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "skills"))
								.description("Discover job entities (skills)").build(),

						// Step 4: Manage skill (create/update)
						ScenarioStep.builder().stepId("step4_manage_skill").actionName("manage_skill")
								.addInputMapping(InputMapping.fromScenarioInput("skill_name", "skill_name"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Manage skill (create only)").build(),

						// Step 5: Create audit log entry
						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id", "manage_skill",
										"skill_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "skills"))
								.description("Create audit log for skill creation").build()))
				.build();
	}

	/**
	 * Build the list of required inputs for the UI.
	 * 
	 * Only essential inputs are requested from the user. Filter patterns are
	 * prepared internally by the scenario.
	 */
	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("johnny.jones@protonmail.com").build(),

				ScenarioInputDefinition.builder().name("skill_name").type("string")
						.description("Name of the skill to manage").required(true).example("Cyber security").build());
	}
}
