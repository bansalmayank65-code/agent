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
 * Create Performance Review Scenario.
 * 
 * This scenario provides a complete workflow for creating performance reviews.
 * 
 * Workflow: 1. Check approval for performance review 2. Discover employee 3.
 * Discover reviewer 4. Create performance review 5. Create audit log entry
 * 
 * Category: Performance Management Environment: hr_experts Interface: 1
 */
public final class CreatePerformanceReviewScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(CreatePerformanceReviewScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public CreatePerformanceReviewScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public CreatePerformanceReviewScenario(Map<String, Object> parameters) {
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
		String employeeId = (String) parameters.get("employee_id");
		String reviewerId = (String) parameters.get("reviewer_id");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("employee_id_filter",
				String.format("{\"employee_id\":\"%s\",\"employment_status\":\"active\"}", employeeId));
		scenarioInputs.put("reviewer_id_filter",
				String.format("{\"employee_id\":\"%s\",\"employment_status\":\"active\"}", reviewerId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building create_performance_review scenario configuration");

		return ScenarioConfig.builder().scenarioName("create_performance_review")
				.description("Complete workflow for creating performance reviews").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "performance_review_creation"))
								.description("Check approval for performance review").build(),

						ScenarioStep.builder().stepId("step3_discover_employee")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "employee_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employees"))
								.description("Discover employee").build(),

						ScenarioStep.builder().stepId("step4_discover_reviewer")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "reviewer_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employees"))
								.description("Discover reviewer").build(),

						ScenarioStep.builder().stepId("step5_create_review").actionName("manage_performance_review")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("reviewer_id", "reviewer_id"))
								.addInputMapping(
										InputMapping.fromScenarioInput("review_period_start", "review_period_start"))
								.addInputMapping(
										InputMapping.fromScenarioInput("review_period_end", "review_period_end"))
								.addInputMapping(InputMapping.fromScenarioInput("review_type", "review_type"))
								.addInputMapping(InputMapping.fromScenarioInput("overall_rating", "overall_rating"))
								.addInputMapping(InputMapping.fromScenarioInput("goals_achievement_score",
										"goals_achievement_score"))
								.addInputMapping(
										InputMapping.fromScenarioInput("communication_score", "communication_score"))
								.addInputMapping(InputMapping.fromScenarioInput("teamwork_score", "teamwork_score"))
								.addInputMapping(InputMapping.fromScenarioInput("leadership_score", "leadership_score"))
								.addInputMapping(InputMapping.fromScenarioInput("technical_skills_score",
										"technical_skills_score"))
								.addInputMapping(InputMapping.withStaticValue("status", "draft"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create performance review").build(),

						ScenarioStep.builder().stepId("step6_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_performance_review", "review_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "performance_reviews"))
								.description("Create audit log for performance review").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("hr.manager@company.com").build(),

				ScenarioInputDefinition.builder().name("employee_id").type("string")
						.description("ID of the employee being reviewed").required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("reviewer_id").type("string").description("ID of the reviewer")
						.required(true).example("67890").build(),

				ScenarioInputDefinition.builder().name("review_period_start").type("date")
						.description("Start date of review period").required(true).example("2025-01-01").build(),

				ScenarioInputDefinition.builder().name("review_period_end").type("date")
						.description("End date of review period").required(true).example("2025-12-31").build(),

				ScenarioInputDefinition.builder().name("review_type").type("string")
						.description("Type of review (annual, quarterly, probationary, mid_year)").required(true)
						.example("annual").build(),

				ScenarioInputDefinition.builder().name("overall_rating").type("number")
						.description("Overall rating (1-5)").required(true).example("4").build(),

				ScenarioInputDefinition.builder().name("goals_achievement_score").type("number")
						.description("Goals achievement score (1-5)").required(false).example("4").build(),

				ScenarioInputDefinition.builder().name("communication_score").type("number")
						.description("Communication score (1-5)").required(false).example("5").build(),

				ScenarioInputDefinition.builder().name("teamwork_score").type("number")
						.description("Teamwork score (1-5)").required(false).example("4").build(),

				ScenarioInputDefinition.builder().name("leadership_score").type("number")
						.description("Leadership score (1-5)").required(false).example("4").build(),

				ScenarioInputDefinition.builder().name("technical_skills_score").type("number")
						.description("Technical skills score (1-5)").required(false).example("5").build());
	}
}
