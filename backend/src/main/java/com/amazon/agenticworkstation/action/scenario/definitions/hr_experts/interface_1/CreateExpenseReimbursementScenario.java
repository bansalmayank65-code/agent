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
 * Create Expense Reimbursement Scenario.
 * 
 * This scenario provides a complete workflow for creating expense reimbursement
 * requests.
 * 
 * Workflow: 1. Discover employee 2. Create expense reimbursement request 3.
 * Create audit log entry
 * 
 * Category: Expense Management Environment: hr_experts Interface: 1
 */
public final class CreateExpenseReimbursementScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(CreateExpenseReimbursementScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public CreateExpenseReimbursementScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public CreateExpenseReimbursementScenario(Map<String, Object> parameters) {
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
		log.debug("Building create_expense_reimbursement scenario configuration");

		return ScenarioConfig.builder().scenarioName("create_expense_reimbursement")
				.description("Complete workflow for creating expense reimbursement requests").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(
						ScenarioStep.builder().stepId("step1_discover_user")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_discover_employee")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "employee_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "employees"))
								.description("Discover employee").build(),

						ScenarioStep.builder().stepId("step3_create_expense_request")
								.actionName("manage_expense_reimbursement")
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("expense_type", "expense_type"))
								.addInputMapping(InputMapping.fromScenarioInput("amount", "amount"))
								.addInputMapping(InputMapping.fromScenarioInput("expense_date", "expense_date"))
								.addInputMapping(InputMapping.fromScenarioInput("receipt_path", "receipt_path"))
								.addInputMapping(InputMapping.withStaticValue("status", "pending"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Create expense reimbursement request").build(),

						ScenarioStep.builder().stepId("step4_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_expense_reimbursement", "expense_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(
										InputMapping.withStaticValue("reference_type", "expense_reimbursements"))
								.description("Create audit log for expense submission").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("employee_id").type("string").description("ID of the employee")
						.required(true).example("54321").build(),

				ScenarioInputDefinition.builder().name("expense_type").type("string")
						.description("Type of expense (travel, meals, lodging, office_supplies, training, mileage)")
						.required(true).example("travel").build(),

				ScenarioInputDefinition.builder().name("amount").type("number").description("Expense amount")
						.required(true).example("350.00").build(),

				ScenarioInputDefinition.builder().name("expense_date").type("date").description("Date of expense")
						.required(true).example("2025-10-15").build(),

				ScenarioInputDefinition.builder().name("receipt_path").type("string")
						.description("Path to receipt document").required(true)
						.example("/receipts/travel_receipt_001.pdf").build());
	}
}
