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
 * User Provisioning Scenario.
 * 
 * This scenario provides a complete workflow for provisioning new users in the
 * HR system.
 * 
 * Workflow: 1. Check approval for user provisioning 2. Create the new user 3.
 * Create audit log entry
 * 
 * Category: User Management Environment: hr_experts Interface: 1
 */
public final class UserProvisioningScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(UserProvisioningScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public UserProvisioningScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public UserProvisioningScenario(Map<String, Object> parameters) {
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
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building user_provisioning scenario configuration");

		return ScenarioConfig.builder().scenarioName("user_provisioning")
				.description("Complete workflow for provisioning new users").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "user_provisioning"))
								.description("Check approval for user provisioning").build(),

						ScenarioStep.builder().stepId("step3_manage_user").actionName("manage_user")
								.addInputMapping(InputMapping.fromScenarioInput("first_name", "first_name"))
								.addInputMapping(InputMapping.fromScenarioInput("last_name", "last_name"))
								.addInputMapping(InputMapping.fromScenarioInput("email", "email"))
								.addInputMapping(InputMapping.fromScenarioInput("role", "role"))
								.addInputMapping(InputMapping.fromScenarioInput("phone_number", "phone_number"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create the new user").build(),

						ScenarioStep.builder().stepId("step4_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(
										InputMapping.fromPreviousActionOutput("reference_id", "manage_user", "user_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "users"))
								.description("Create audit log for user provisioning").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.director@company.com").build(),

				ScenarioInputDefinition.builder().name("first_name").type("string")
						.description("First name of the new user").required(true).example("John").build(),

				ScenarioInputDefinition.builder().name("last_name").type("string")
						.description("Last name of the new user").required(true).example("Doe").build(),

				ScenarioInputDefinition.builder().name("email").type("email")
						.description("Email address for the new user").required(true).example("john.doe@company.com")
						.build(),

				ScenarioInputDefinition.builder().name("role").type("string").description("Role for the new user")
						.required(true).example("employee").build(),

				ScenarioInputDefinition.builder().name("phone_number").type("string")
						.description("Phone number of the new user").required(false).example("+1-555-0123").build());
	}
}
