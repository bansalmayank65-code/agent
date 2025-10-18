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
 * Payroll Deduction Management Scenario.
 * 
 * This scenario provides a complete workflow for managing payroll deductions.
 * 
 * Workflow: 1. Discover employee 2. Create payroll deduction 3. Create audit
 * log entry
 * 
 * Category: Payroll Management Environment: hr_experts Interface: 1
 */
public final class PayrollDeductionManagementScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(PayrollDeductionManagementScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public PayrollDeductionManagementScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public PayrollDeductionManagementScenario(Map<String, Object> parameters) {
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
		log.debug("Building payroll_deduction_management scenario configuration");

		return ScenarioConfig.builder().scenarioName("payroll_deduction_management")
				.description("Complete workflow for managing payroll deductions").envName("hr_experts")
				.interfaceNumber(1)
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

						ScenarioStep.builder().stepId("step3_create_deduction").actionName("manage_payroll_deduction")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("deduction_type", "deduction_type"))
								.addInputMapping(InputMapping.fromScenarioInput("amount", "amount"))
								.addInputMapping(InputMapping.fromScenarioInput("start_date", "start_date"))
								.addInputMapping(InputMapping.fromScenarioInput("end_date", "end_date"))
								.addInputMapping(InputMapping.fromScenarioInput("is_recurring", "is_recurring"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create payroll deduction").build(),

						ScenarioStep.builder().stepId("step4_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_payroll_deduction", "deduction_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "payroll_deductions"))
								.description("Create audit log for payroll deduction").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("employee_id").type("string").description("ID of the employee")
						.required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("deduction_type").type("string")
						.description("Type of deduction (tax, health_insurance, retirement, garnishment, other)")
						.required(true).example("retirement").build(),

				ScenarioInputDefinition.builder().name("amount").type("number").description("Deduction amount")
						.required(true).example("500.00").build(),

				ScenarioInputDefinition.builder().name("start_date").type("date")
						.description("Start date for deduction").required(true).example("2025-11-01").build(),

				ScenarioInputDefinition.builder().name("end_date").type("date").description("End date for deduction")
						.required(false).example("2026-11-01").build(),

				ScenarioInputDefinition.builder().name("is_recurring").type("boolean")
						.description("Is this a recurring deduction").required(false).example("true").build());
	}
}
