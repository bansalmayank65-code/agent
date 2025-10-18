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
 * Payroll Correction Scenario.
 * 
 * This scenario provides a complete workflow for correcting payroll records.
 * 
 * Workflow: 1. Check approval for payroll correction 2. Discover payroll record
 * 3. Update payroll record with corrections 4. Create audit log entry
 * 
 * Category: Payroll Management Environment: hr_experts Interface: 1
 */
public final class PayrollCorrectionScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(PayrollCorrectionScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public PayrollCorrectionScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public PayrollCorrectionScenario(Map<String, Object> parameters) {
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
		String payrollId = (String) parameters.get("payroll_id");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", requesterEmail));
		scenarioInputs.put("payroll_id_filter", String.format("{\"payroll_id\":\"%s\"}", payrollId));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building payroll_correction scenario configuration");

		return ScenarioConfig.builder().scenarioName("payroll_correction")
				.description("Complete workflow for correcting payroll records").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_check_approval").actionName("check_approval")
								.addInputMapping(InputMapping.fromScenarioInput("requester_email", "requester_email"))
								.addInputMapping(InputMapping.withStaticValue("action", "payroll_correction"))
								.description("Check approval for payroll correction").build(),

						ScenarioStep.builder().stepId("step3_discover_payroll").actionName("discover_payroll_entities")
								.addInputMapping(InputMapping.fromScenarioInput("filters", "payroll_id_filter"))
								.addInputMapping(InputMapping.withStaticValue("entity_type", "payroll_records"))
								.description("Discover payroll record").build(),

						ScenarioStep.builder().stepId("step4_update_payroll").actionName("manage_payroll_record")
								.addInputMapping(InputMapping.fromScenarioInput("payroll_id", "payroll_id"))
								.addInputMapping(
										InputMapping.fromScenarioInput("corrected_gross_pay", "corrected_gross_pay"))
								.addInputMapping(
										InputMapping.fromScenarioInput("corrected_net_pay", "corrected_net_pay"))
								.addInputMapping(
										InputMapping.fromScenarioInput("correction_reason", "correction_reason"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.description("Update payroll record with corrections").build(),

						ScenarioStep.builder().stepId("step5_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromScenarioInput("reference_id", "payroll_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "update"))
								.addInputMapping(InputMapping.withStaticValue("operation", "correct"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "payroll_records"))
								.description("Create audit log for payroll correction").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("requester_email").type("email")
						.description("Email of the person requesting the correction").required(true)
						.example("payroll.admin@company.com").build(),

				ScenarioInputDefinition.builder().name("payroll_id").type("string")
						.description("ID of the payroll record to correct").required(true).example("99887").build(),

				ScenarioInputDefinition.builder().name("corrected_gross_pay").type("number")
						.description("Corrected gross pay amount").required(false).example("5200.00").build(),

				ScenarioInputDefinition.builder().name("corrected_net_pay").type("number")
						.description("Corrected net pay amount").required(false).example("3800.00").build(),

				ScenarioInputDefinition.builder().name("correction_reason").type("string")
						.description("Reason for the correction").required(true)
						.example("Incorrect overtime calculation").build());
	}
}
