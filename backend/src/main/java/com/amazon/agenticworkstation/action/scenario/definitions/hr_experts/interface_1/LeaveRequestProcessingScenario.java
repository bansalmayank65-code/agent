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
 * Leave Request Processing Scenario.
 * 
 * This scenario provides a complete workflow for leave request processing.
 * 
 * Workflow: 1. Discover employee 2. Create leave request 3. Create audit log
 * entry
 * 
 * Category: Leave Management Environment: hr_experts Interface: 1
 */
public final class LeaveRequestProcessingScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(LeaveRequestProcessingScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public LeaveRequestProcessingScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public LeaveRequestProcessingScenario(Map<String, Object> parameters) {
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
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("employee_id_filter",
				String.format("{\"employee_id\":\"%s\",\"employment_status\":\"active\"}", employeeId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building leave_request_processing scenario configuration");

		return ScenarioConfig.builder().scenarioName("leave_request_processing")
				.description("Complete workflow for leave request processing").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_discover_employee")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "employee_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employees"))
								.description("Discover employee").build(),

						ScenarioStep.builder().stepId("step3_create_leave_request").actionName("manage_leave_requests")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("leave_type", "leave_type"))
								.addInputMapping(InputMapping.fromScenarioInput("start_date", "start_date"))
								.addInputMapping(InputMapping.fromScenarioInput("end_date", "end_date"))
								.addInputMapping(InputMapping.fromScenarioInput("requested_days", "requested_days"))
								.addInputMapping(InputMapping.withStaticValue("status", "pending"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create leave request").build(),

						ScenarioStep.builder().stepId("step4_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_leave_requests", "leave_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "leave_requests"))
								.description("Create audit log for leave request submission").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("employee_id").type("string").description("ID of the employee")
						.required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("leave_type").type("string").description(
						"Type of leave (vacation, sick, personal, maternity, paternity, bereavement, jury_duty)")
						.required(true).example("vacation").build(),

				ScenarioInputDefinition.builder().name("start_date").type("date").description("Start date of leave")
						.required(true).example("2025-11-01").build(),

				ScenarioInputDefinition.builder().name("end_date").type("date").description("End date of leave")
						.required(true).example("2025-11-05").build(),

				ScenarioInputDefinition.builder().name("requested_days").type("number")
						.description("Number of days requested").required(false).example("5").build());
	}
}
