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
 * Process Payroll Run Scenario.
 * 
 * This scenario provides a complete workflow for processing payroll runs.
 * 
 * Workflow: 1. Check approval for payroll processing 2. Discover approved
 * timesheets 3. Create payroll record 4. Create audit log entry
 * 
 * Category: Payroll Management Environment: hr_experts Interface: 1
 */
public final class ProcessPayrollRunScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(ProcessPayrollRunScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public ProcessPayrollRunScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public ProcessPayrollRunScenario(Map<String, Object> parameters) {
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
		String payPeriodStart = (String) parameters.get("pay_period_start");
		String payPeriodEnd = (String) parameters.get("pay_period_end");

		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("timesheet_filter", String.format(
				"{\"employee_id\":\"%s\",\"work_date_start\":\"%s\",\"work_date_end\":\"%s\",\"status\":\"approved\"}",
				employeeId, payPeriodStart, payPeriodEnd));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building process_payroll_run scenario configuration");

		return ScenarioConfig.builder().scenarioName("process_payroll_run")
				.description("Complete workflow for processing payroll runs").envName("hr_experts").interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "payroll_processing"))
								.description("Check approval for payroll processing").build(),

						ScenarioStep.builder().stepId("step3_discover_timesheets")
								.actionName("discover_timesheet_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "timesheet_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "timesheets"))
								.description("Discover approved timesheets").build(),

						ScenarioStep.builder().stepId("step4_create_payroll").actionName("manage_payroll_record")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("pay_period_start", "pay_period_start"))
								.addInputMapping(InputMapping.fromScenarioInput("pay_period_end", "pay_period_end"))
								.addInputMapping(InputMapping.fromScenarioInput("hourly_rate", "hourly_rate"))
								.addInputMapping(InputMapping.fromScenarioInput("hours_worked", "hours_worked"))
								.addInputMapping(InputMapping.fromScenarioInput("payment_date", "payment_date"))
								.addInputMapping(InputMapping.withStaticValue("status", "pending"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create payroll record").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_payroll_record", "payroll_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "payroll_records"))
								.description("Create audit log for payroll transactions").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the action").required(true)
						.example("finance@company.com").build(),

				ScenarioInputDefinition.builder().name("employee_id").type("string").description("ID of the employee")
						.required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("pay_period_start").type("date")
						.description("Start date of the pay period").required(true).example("2025-10-01").build(),

				ScenarioInputDefinition.builder().name("pay_period_end").type("date")
						.description("End date of the pay period").required(true).example("2025-10-15").build(),

				ScenarioInputDefinition.builder().name("hourly_rate").type("number")
						.description("Hourly rate for the employee").required(true).example("65").build(),

				ScenarioInputDefinition.builder().name("hours_worked").type("number")
						.description("Total hours worked in pay period").required(false).example("80").build(),

				ScenarioInputDefinition.builder().name("payment_date").type("date").description("Payment date")
						.required(false).example("2025-10-20").build());
	}
}
