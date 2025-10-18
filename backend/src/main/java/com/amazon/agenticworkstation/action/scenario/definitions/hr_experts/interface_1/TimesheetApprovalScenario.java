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
 * Timesheet Approval Scenario.
 * 
 * This scenario provides a complete workflow for timesheet approval/correction.
 * 
 * Workflow: 1. Check approval authority 2. Discover timesheet 3. Discover
 * approver 4. Update timesheet with approval/corrections 5. Create audit log
 * entry
 * 
 * Category: Timesheet Management Environment: hr_experts Interface: 1
 */
public final class TimesheetApprovalScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(TimesheetApprovalScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public TimesheetApprovalScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public TimesheetApprovalScenario(Map<String, Object> parameters) {
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
		String timesheetId = (String) parameters.get("timesheet_id");
		String approverEmail = (String) parameters.get("approver_email");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("timesheet_id_filter", String.format("{\"timesheet_id\":\"%s\"}", timesheetId));
		scenarioInputs.put("approver_email_filter", String.format("{\"email\":\"%s\"}", approverEmail));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building timesheet_approval scenario configuration");

		return ScenarioConfig.builder().scenarioName("timesheet_approval")
				.description("Complete workflow for timesheet approval and correction").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(
						ScenarioStep.builder().stepId("step1_discover_user")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "approver_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "timesheet_approval"))
								.description("Check approval authority").build(),

						ScenarioStep.builder().stepId("step3_discover_timesheet")
								.actionName("discover_timesheet_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "timesheet_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "timesheets"))
								.description("Discover timesheet").build(),

						ScenarioStep.builder().stepId("step4_discover_approver")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "approver_email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover approver").build(),

						ScenarioStep.builder().stepId("step5_update_timesheet").actionName("manage_timesheet_entries")
								.addInputMapping(InputMapping.fromScenarioInput("timesheet_id", "timesheet_id"))
								.addInputMapping(InputMapping.fromScenarioInput("status", "new_status"))
								.addInputMapping(InputMapping.fromScenarioInput("clock_in_time", "clock_in_time"))
								.addInputMapping(InputMapping.fromScenarioInput("clock_out_time", "clock_out_time"))
								.addInputMapping(InputMapping.fromScenarioInput("break_duration_minutes",
										"break_duration_minutes"))
								.addInputMapping(InputMapping.fromScenarioInput("total_hours", "total_hours"))
								.addInputMapping(InputMapping.fromScenarioInput("project_code", "project_code"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update timesheet with approval/corrections").build(),

						ScenarioStep.builder().stepId("step6_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_timesheet_entries", "timesheet_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.addInputMapping(InputMapping.withStaticValue("operation", "approve"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "timesheets"))
								.description("Create audit log for approval and corrections").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("timesheet_id").type("string").description("ID of the timesheet")
						.required(true).example("76543").build(),

				ScenarioInputDefinition.builder().name("approver_email").type("email")
						.description("Email of the approver").required(true).example("manager@company.com").build(),

				ScenarioInputDefinition.builder().name("new_status").type("string")
						.description("New status (submitted, approved, rejected)").required(true).example("approved")
						.build(),

				ScenarioInputDefinition.builder().name("clock_in_time").type("time")
						.description("Corrected clock in time").required(false).example("09:00:00").build(),

				ScenarioInputDefinition.builder().name("clock_out_time").type("time")
						.description("Corrected clock out time").required(false).example("17:00:00").build(),

				ScenarioInputDefinition.builder().name("break_duration_minutes").type("number")
						.description("Corrected break duration").required(false).example("60").build(),

				ScenarioInputDefinition.builder().name("total_hours").type("number")
						.description("Corrected total hours").required(false).example("8").build(),

				ScenarioInputDefinition.builder().name("project_code").type("string")
						.description("Corrected project code").required(false).example("PRJ-001").build());
	}
}
