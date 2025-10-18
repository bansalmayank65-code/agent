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
 * Document Upload Scenario.
 * 
 * This scenario provides a complete workflow for document upload and
 * management.
 * 
 * Workflow: 1. Upload and store document 2. Create audit log entry
 * 
 * Category: Document Management Environment: hr_experts Interface: 1
 */
public final class DocumentUploadScenario implements BaseScenario {
	private static final Logger log = LoggerFactory.getLogger(DocumentUploadScenario.class);
	private static final ScenarioConfig config = buildScenarioConfig();
	private static final List<ScenarioInputDefinition> requiredInputs = buildRequiredInputs();

	private final Map<String, Object> parameters;

	public DocumentUploadScenario() {
		this.parameters = new LinkedHashMap<>();
	}

	public DocumentUploadScenario(Map<String, Object> parameters) {
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

		String uploadedBy = (String) parameters.get("uploaded_by");
		scenarioInputs.put("email_filter", String.format("{\"email\":\"%s\"}", uploadedBy));

		return scenarioInputs;
	}

	private static ScenarioConfig buildScenarioConfig() {
		log.debug("Building document_upload scenario configuration");

		return ScenarioConfig.builder().scenarioName("document_upload")
				.description("Complete workflow for document upload and management").envName("hr_experts")
				.interfaceNumber(1)
				.steps(Arrays.asList(ScenarioStep.builder().stepId("step1_discover_user")
						.actionName("discover_user_employee_entities")
						.addInputMapping(InputMapping.fromScenarioInput("filters", "email_filter"))
						.addInputMapping(InputMapping.withStaticValue("entity_type", "users"))
						.description("Discover user/employee entities by email").build(),

						ScenarioStep.builder().stepId("step2_upload_document").actionName("manage_document_storage")
								.addInputMapping(InputMapping.fromScenarioInput("document_name", "document_name"))
								.addInputMapping(InputMapping.fromScenarioInput("document_type", "document_type"))
								.addInputMapping(InputMapping.fromScenarioInput("file_path", "file_path"))
								.addInputMapping(InputMapping.fromScenarioInput("uploaded_by", "uploaded_by"))
								.addInputMapping(InputMapping.fromScenarioInput("confidentiality_level",
										"confidentiality_level"))
								.addInputMapping(InputMapping.fromScenarioInput("retention_period_years",
										"retention_period_years"))
								.addInputMapping(InputMapping.fromScenarioInput("employee_id", "employee_id"))
								.addInputMapping(InputMapping.fromScenarioInput("expiry_date", "expiry_date"))
								.addInputMapping(InputMapping.withStaticValue("status", "active"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.description("Upload and store document").build(),

						ScenarioStep.builder().stepId("step3_audit_log").actionName("manage_audit_logs")
								.addInputMapping(InputMapping.fromPreviousStepOutput("user_id", "step1_discover_user",
										"results[0].user_id"))
								.addInputMapping(InputMapping.fromPreviousActionOutput("reference_id",
										"manage_document_storage", "document_id"))
								.addInputMapping(InputMapping.withStaticValue("action", "create"))
								.addInputMapping(InputMapping.withStaticValue("operation", "create"))
								.addInputMapping(InputMapping.withStaticValue("reference_type", "documents"))
								.description("Create audit log for document creation").build()))
				.build();
	}

	private static List<ScenarioInputDefinition> buildRequiredInputs() {
		return Arrays.asList(
				ScenarioInputDefinition.builder().name("document_name").type("string")
						.description("Name of the document").required(true).example("Employee Handbook 2025").build(),

				ScenarioInputDefinition.builder().name("document_type").type("string")
						.description("Type of document (contract, policy, handbook, form, certificate, report)")
						.required(true).example("handbook").build(),

				ScenarioInputDefinition.builder().name("file_path").type("string").description("File path or URL")
						.required(true).example("/documents/handbook_2025.pdf").build(),

				ScenarioInputDefinition.builder().name("uploaded_by").type("string").description("ID of the uploader")
						.required(true).example("67890").build(),

				ScenarioInputDefinition.builder().name("confidentiality_level").type("string")
						.description("Confidentiality level (public, internal, confidential, restricted)")
						.required(true).example("internal").build(),

				ScenarioInputDefinition.builder().name("retention_period_years").type("number")
						.description("Retention period in years").required(true).example("7").build(),

				ScenarioInputDefinition.builder().name("employee_id").type("string")
						.description("Associated employee ID (if applicable)").required(false).example("54321").build(),

				ScenarioInputDefinition.builder().name("expiry_date").type("date").description("Document expiry date")
						.required(false).example("2030-12-31").build());
	}
}
