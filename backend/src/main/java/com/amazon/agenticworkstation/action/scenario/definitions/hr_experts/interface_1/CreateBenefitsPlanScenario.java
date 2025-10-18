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
 * Create Benefits Plan Scenario.
 * 
 * This scenario provides a complete workflow for creating benefits plans.
 * 
 * Workflow: 1. Check approval for benefits plan creation 2. Create benefits
 * plan 3. Create audit log entry
 * 
 * Category: Benefits Management Environment: hr_experts Interface: 1
 */
public final class CreateBenefitsPlanScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(CreateBenefitsPlanScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public CreateBenefitsPlanScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public CreateBenefitsPlanScenario(Map<String, Object> parameters) {
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
		log.debug("Building create_benefits_plan scenario configuration");

		return ScenarioConfig.builder().scenarioName("create_benefits_plan")
				.description("Complete workflow for creating benefits plans").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "benefits_plan_creation"))
								.description("Check approval for benefits plan creation").build(),

						ScenarioStep.builder().stepId("step3_manage_benefits_plan").actionName("manage_benefits_plan")
								.addInputMapping(InputMapping.fromScenarioInput("plan_name", "plan_name"))
								.addInputMapping(InputMapping.fromScenarioInput("plan_type", "plan_type"))
								.addInputMapping(InputMapping.fromScenarioInput("effective_date", "effective_date"))
								.addInputMapping(InputMapping.fromScenarioInput("provider", "provider"))
								.addInputMapping(InputMapping.fromScenarioInput("employee_cost", "employee_cost"))
								.addInputMapping(InputMapping.fromScenarioInput("employer_cost", "employer_cost"))
								.addInputMapping(InputMapping.fromScenarioInput("expiration_date", "expiration_date"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create benefits plan").build(),

						ScenarioStep.builder().stepId("step4_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_benefits_plan", "plan_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "benefits_plans"))
								.description("Create audit log for benefits plan operation").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.director@company.com").build(),

				ScenarioInputDefinition.builder().name("plan_name").type("string")
						.description("Name of the benefits plan").required(true).example("Premium Health Plan").build(),

				ScenarioInputDefinition.builder().name("plan_type").type("string")
						.description("Type of plan (health, dental, vision, retirement, life_insurance)").required(true)
						.example("health").build(),

				ScenarioInputDefinition.builder().name("effective_date").type("date")
						.description("Effective date of the plan").required(true).example("2025-11-01").build(),

				ScenarioInputDefinition.builder().name("provider").type("string").description("Benefits provider")
						.required(false).example("HealthCare Inc.").build(),

				ScenarioInputDefinition.builder().name("employee_cost").type("number")
						.description("Employee cost per month").required(false).example("200").build(),

				ScenarioInputDefinition.builder().name("employer_cost").type("number")
						.description("Employer cost per month").required(false).example("400").build(),

				ScenarioInputDefinition.builder().name("expiration_date").type("date")
						.description("Expiration date of the plan").required(false).example("2026-10-31").build());
	}
}
