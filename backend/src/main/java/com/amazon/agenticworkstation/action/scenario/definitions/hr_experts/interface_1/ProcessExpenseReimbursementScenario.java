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
 * Process Expense Reimbursement Scenario.
 * 
 * This scenario provides a complete workflow for processing expense
 * reimbursement requests.
 * 
 * Workflow: 1. Check approval for expense processing 2. Discover expense
 * request 3. Update expense status (approved/rejected) 4. Create audit log
 * entry
 * 
 * Category: Expense Management Environment: hr_experts Interface: 1
 */
public final class ProcessExpenseReimbursementScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(ProcessExpenseReimbursementScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public ProcessExpenseReimbursementScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public ProcessExpenseReimbursementScenario(Map<String, Object> parameters) {
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
		String expenseId = (String) parameters.get("expense_id");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("expense_id_filter",
				String.format("{\"expense_id\":\"%s\",\"status\":\"pending\"}", expenseId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building process_expense_reimbursement scenario configuration");

		return ScenarioConfig.builder().scenarioName("process_expense_reimbursement")
				.description("Complete workflow for processing expense reimbursement requests").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(
						ScenarioStep.builder().stepId("step1_discover_user")
								.actionName("discover_user_employee_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
								.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(
										InputMapping.withStaticValue("action", "expense_reimbursement_processing"))
								.description("Check approval for expense processing").build(),

						ScenarioStep.builder().stepId("step3_discover_expense").actionName("discover_expense_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "expense_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "expense_reimbursements"))
								.description("Discover expense request").build(),

						ScenarioStep.builder().stepId("step4_update_expense_status")
								.actionName("manage_expense_reimbursement")
								.addInputMapping(InputMapping.fromScenarioInput("expense_id", "expense_id"))
								.addInputMapping(InputMapping.fromScenarioInput("status", "status"))
								.addInputMapping(InputMapping.fromScenarioInput("approved_amount", "approved_amount"))
								.addInputMapping(InputMapping.fromScenarioInput("payment_date", "payment_date"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update expense status").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromScenarioInput("reference_id", "expense_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.addInputMapping(InputMapping.withStaticValue("operation", "process"))
								.addInputMapping(
										InputMapping.withStaticValue("reference_type", "expense_reimbursements"))
								.description("Create audit log for expense processing").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person processing the expense").required(true)
						.example("manager@company.com").build(),

				ScenarioInputDefinition.builder().name("expense_id").type("string")
						.description("ID of the expense request").required(true).example("99887").build(),

				ScenarioInputDefinition.builder().name("status").type("string")
						.description("Approval status (approved, rejected, pending)").required(true).example("approved")
						.build(),

				ScenarioInputDefinition.builder().name("approved_amount").type("number").description("Approved amount")
						.required(false).example("350.00").build(),

				ScenarioInputDefinition.builder().name("payment_date").type("date").description("Payment date")
						.required(false).example("2025-10-25").build());
	}
}
