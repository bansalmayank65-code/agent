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
 * Create Training Program Scenario.
 * 
 * This scenario provides a complete workflow for creating training programs.
 * 
 * Workflow: 1. Create training program 2. Create audit log entry
 * 
 * Category: Training Management Environment: hr_experts Interface: 1
 */
public final class CreateTrainingProgramScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(CreateTrainingProgramScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public CreateTrainingProgramScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public CreateTrainingProgramScenario(Map<String, Object> parameters) {
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
		log.debug("Building create_training_program scenario configuration");

		return ScenarioConfig.builder().scenarioName("create_training_program")
				.description("Complete workflow for creating training programs").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_create_program").actionName("manage_training_programs")
								.addInputMapping(InputMapping.fromScenarioInput("program_name", "program_name"))
								.addInputMapping(InputMapping.fromScenarioInput("program_type", "program_type"))
								.addInputMapping(InputMapping.fromScenarioInput("duration_hours", "duration_hours"))
								.addInputMapping(InputMapping.fromScenarioInput("delivery_method", "delivery_method"))
								.addInputMapping(InputMapping.fromScenarioInput("mandatory", "mandatory"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create training program").build(),

						ScenarioStep.builder().stepId("step3_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_training_programs", "program_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "training_programs"))
								.description("Create audit log").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.manager@company.com").build(),

				ScenarioInputDefinition.builder().name("program_name").type("string")
						.description("Name of the training program").required(true).example("Leadership Development")
						.build(),

				ScenarioInputDefinition.builder().name("program_type").type("string")
						.description("Type of program (onboarding, compliance, technical, leadership, safety)")
						.required(true).example("leadership").build(),

				ScenarioInputDefinition.builder().name("duration_hours").type("number").description("Duration in hours")
						.required(true).example("40").build(),

				ScenarioInputDefinition.builder().name("delivery_method").type("string")
						.description("Delivery method (online, in_person, hybrid)").required(true).example("hybrid")
						.build(),

				ScenarioInputDefinition.builder().name("mandatory").type("boolean")
						.description("Is the training mandatory").required(false).example("false").build());
	}
}
