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
 * Add Candidate Record Scenario.
 * 
 * This scenario provides a complete workflow for adding candidate records.
 * 
 * Workflow: 1. Discover existing candidate by email 2. Create candidate record
 * 3. Create audit log entry
 * 
 * Category: Recruitment Management Environment: hr_experts Interface: 1
 */
public final class AddCandidateRecordScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(AddCandidateRecordScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public AddCandidateRecordScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public AddCandidateRecordScenario(Map<String, Object> parameters) {
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
		String email = (String) parameters.get("email");
		scenarioInputs.put("requester_email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", email));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building add_candidate_record scenario configuration");

		return ScenarioConfig
				.builder().scenarioName(
						"add_candidate_record")
				.description("Complete workflow for adding candidate records").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(
						ScenarioStep.builder().stepId("step1_discover_user")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "requester_email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_discover_candidate")
								.actionName("discover_recruitment_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "candidates"))
								.description("Discover existing candidate by email").build(),

						ScenarioStep.builder().stepId("step3_manage_candidate").actionName("manage_candidate")
								.addInputMapping(InputMapping.fromScenarioInput("first_name", "first_name"))
								.addInputMapping(InputMapping.fromScenarioInput("last_name", "last_name"))
								.addInputMapping(InputMapping.fromScenarioInput("email", "email"))
								.addInputMapping(InputMapping.fromScenarioInput("source", "source"))
								.addInputMapping(InputMapping.fromScenarioInput("phone_number", "phone_number"))	
								.addInputMapping(InputMapping.fromScenarioInput("address", "address"))
								.addInputMapping(InputMapping.withStaticValue("status", "new"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create candidate record").build(),

						ScenarioStep.builder().stepId("step4_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_candidate", "candidate_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "candidates"))
								.description("Create audit log for candidate creation").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("first_name").type("string")
						.description("First name of the candidate").required(true).example("Jane").build(),

				ScenarioInputDefinition.builder().name("last_name").type("string")
						.description("Last name of the candidate").required(true).example("Smith").build(),

				ScenarioInputDefinition.builder().name("email").type("email")
						.description("Email address of the candidate").required(true).example("jane.smith@email.com")
						.build(),

				ScenarioInputDefinition.builder().name("source").type("string")
						.description(
								"Source of the candidate (website, referral, linkedin, job_board, recruiter, other)")
						.required(true).example("linkedin").build(),

				ScenarioInputDefinition.builder().name("phone_number").type("string")
						.description("Phone number of the candidate").required(false).example("+1-555-0124").build(),

				ScenarioInputDefinition.builder().name("address").type("string").description("Address of the candidate")
						.required(false).example("123 Main St, City, State").build());
	}
}
