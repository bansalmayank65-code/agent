package com.amazon.agenticworkstation.action;

import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.INTERFACE_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.action.scenario.models.PythonFunctionMetadata;
import com.amazon.agenticworkstation.dto.TaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service to generate actions by executing Python functions sequentially and
 * chaining outputs between actions.
 * 
 * This is the main orchestrator that delegates to specialized components: -
 * DataFileManager: Handles data file preparation - InterfaceMappingLoader:
 * Loads and caches interface mappings - ArgumentBuilder: Builds arguments for
 * actions - AuditLogGenerator: Generates audit log actions
 */
public final class ActionGenerator {
	private static final Logger log = LoggerFactory.getLogger(ActionGenerator.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private ActionGenerator() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Generate actions by executing Python functions in sequence with explicit
	 * environment and interface
	 * 
	 * @param actionNames       List of action names (function names) to execute in
	 *                          sequence
	 * @param actionInputs      Map of action-specific inputs (action name -> field
	 *                          name -> value)
	 * @param instructionInputs Map of field names to values extracted from
	 *                          instruction (with proper field names)
	 * @param envName           Environment name (e.g., "hr_experts")
	 * @param interfaceNumber   Interface number (1-5)
	 * @return List of ActionDto with populated arguments and outputs
	 * @throws IOException if Python execution fails or required fields are missing
	 */
	public static List<TaskDto.ActionDto> generateActions(List<String> actionNames,
			Map<String, Map<String, String>> actionInputs, Map<String, String> instructionInputs, String envName,
			int interfaceNumber) throws IOException {

		log.info("Starting action generation for {} actions in env: '{}' interface: {}", actionNames.size(), envName,
				interfaceNumber);
		log.debug("Action names: {}", actionNames);
		log.debug("Action-specific inputs: {}", actionInputs);
		log.debug("Instruction inputs: {}", instructionInputs);

		// Load environment data from environment directory
		String actualDataFile = DataFileManager.prepareDataFile(envName);
		log.info("Using temporary data file for this session: {}", actualDataFile);

		// Get interface-specific configuration
		String interfaceName = INTERFACE_PREFIX + interfaceNumber;
		Map<String, Object> interfaceMappings = InterfaceMappingLoader.loadInterfaceMappings(envName);
		log.info("Using interface: {} for environment: {}", interfaceName, envName);

		// Read auto-audit setting from interface mappings
		boolean autoAuditEnabled = InterfaceMappingLoader.extractAutoAuditSetting(interfaceMappings, envName);
		log.info("Auto-audit enabled for environment '{}': {}", envName, autoAuditEnabled);

		// Extract required fields for all actions using InputFieldExtractor
		Map<String, Map<String, String>> actionFieldsMap = InputFieldExtractor.extractRequiredFields(actionNames,
				envName, interfaceNumber);
		log.info("Extracted required fields for {} actions", actionFieldsMap.size());

		List<TaskDto.ActionDto> actions = new ArrayList<>();
		Map<String, Object> previousOutputs = new HashMap<>();

		for (int i = 0; i < actionNames.size(); i++) {
			String actionName = actionNames.get(i);
			log.info("Processing action {}/{}: {}", i + 1, actionNames.size(), actionName);

			try {
				// Get required fields for this action
				Map<String, String> requiredFields = actionFieldsMap.get(actionName);
				if (requiredFields == null) {
					throw new IOException("No required fields found for action: " + actionName);
				}

				log.debug("Required fields for '{}': {}", actionName, requiredFields.keySet());

				// Extract metadata for this action to get parameter types
				PythonFunctionMetadata metadata = PythonExecutorService.extractMetadata(actionName, envName,
						interfaceNumber);

				// Get action-specific inputs for this action
				Map<String, String> currentActionInputs = actionInputs.getOrDefault(actionName, new HashMap<>());

				// Build arguments by matching exact field names
				Map<String, Object> arguments = buildArgumentsFromFields(actionName, requiredFields, metadata,
						currentActionInputs, instructionInputs, previousOutputs);

				log.info("Executing action '{}' with arguments: {}", actionName, arguments);

				// Execute the Python function
				String resultJson = PythonExecutorService.executePythonFunction(actionName, arguments, actualDataFile,
						envName, interfaceNumber);

				log.debug("Action '{}' returned: {}", actionName, resultJson);

				// Parse the result
				Object output = parseOutput(resultJson);

				// Check if the output contains an error
				if (output instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> outputMap = (Map<String, Object>) output;

					if (outputMap.containsKey("error")) {
						String errorMessage = outputMap.get("error").toString();
						log.error("Action '{}' returned error: {}", actionName, errorMessage);

						// Build complete action details for exception message
						StringBuilder errorDetails = new StringBuilder();
						errorDetails.append("Action '").append(actionName).append("' failed: ").append(errorMessage);
						errorDetails.append("\n\nComplete Action Details:");
						errorDetails.append("\n{\n");
						errorDetails.append("  \"name\": \"").append(actionName).append("\",\n");
						errorDetails.append("  \"arguments\": ").append(formatJson(arguments)).append(",\n");
						errorDetails.append("  \"output\": ").append(formatJson(output)).append("\n");
						errorDetails.append("}");

						throw new IOException(errorDetails.toString());
					}
				}

				// Create ActionDto
				TaskDto.ActionDto actionDto = new TaskDto.ActionDto();
				actionDto.setName(actionName);
				actionDto.setArguments(arguments);
				actionDto.setOutput(output);

				actions.add(actionDto);

				// Store output for next action
				previousOutputs.put(actionName, output);

				// If output is a map with specific fields, store them individually
				if (output instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> outputMap = (Map<String, Object>) output;
					for (Map.Entry<String, Object> entry : outputMap.entrySet()) {
						previousOutputs.put(entry.getKey(), entry.getValue());
					}
				}

				log.info("Successfully executed action '{}' ({}/{})", actionName, i + 1, actionNames.size());

				// Auto-generate audit log if this was a CRUD operation
				String performerUserId = null;
				if (autoAuditEnabled && AuditLogGenerator.isCrudOperation(actionName, arguments, interfaceMappings)) {
					TaskDto.ActionDto auditAction = AuditLogGenerator.generateAuditLogAction(actionName, arguments,
							output, actualDataFile, envName, interfaceNumber, interfaceMappings, performerUserId);
					if (auditAction != null) {
						actions.add(auditAction);
						log.info("Auto-generated audit log action for '{}'", actionName);
					} else {
						throw new RuntimeException("Failed to generate audit log action for '" + actionName + "'");
					}
				}
			} catch (IOException e) {
				// If the exception already contains "Complete Action Details", just re-throw it
				// to avoid wrapping it multiple times
				if (e.getMessage() != null && e.getMessage().contains("Complete Action Details")) {
					log.error("Failed to execute action '{}': {}", actionName, e.getMessage().split("\n")[0]);
					throw e;
				}
				// For other IOExceptions, wrap with additional context
				log.error("Failed to execute action '{}': {}", actionName, e.getMessage(), e);
				throw new IOException("Failed to execute action '" + actionName + "': " + e.getMessage(), e);
			} catch (Exception e) {
				// For non-IOException, wrap with context
				log.error("Failed to execute action '{}': {}", actionName, e.getMessage(), e);
				throw new IOException("Failed to execute action '" + actionName + "': " + e.getMessage(), e);
			}
		}

		log.info("Successfully generated {} actions (including auto-audit)", actions.size());
		return actions;
	}

	/**
	 * Parse Python function output
	 */
	private static Object parseOutput(String resultJson) {
		try {
			// Try to parse as JSON
			return objectMapper.readValue(resultJson, Object.class);
		} catch (Exception e) {
			log.warn("Could not parse output as JSON, returning as string: {}", resultJson);
			return resultJson;
		}
	}

	/**
	 * Format object as pretty-printed JSON string
	 */
	private static String formatJson(Object obj) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
		} catch (Exception e) {
			return obj != null ? obj.toString() : "null";
		}
	}

	/**
	 * Build arguments for an action by matching exact field names with priority
	 * order: 1. Action-specific inputs (highest priority) 2. Instruction inputs 3.
	 * Previous outputs (lowest priority)
	 * 
	 * @param actionName        Action name being executed
	 * @param requiredFields    Map of required field names for this action
	 * @param metadata          Python function metadata with parameter type
	 *                          information
	 * @param actionInputs      Map of action-specific inputs for this action
	 * @param instructionInputs Map of inputs from instruction (with proper field
	 *                          names)
	 * @param previousOutputs   Map of outputs from previous actions
	 * @return Map of arguments populated with values
	 * @throws IOException if required fields cannot be filled
	 */
	private static Map<String, Object> buildArgumentsFromFields(String actionName, Map<String, String> requiredFields,
			PythonFunctionMetadata metadata, Map<String, String> actionInputs, Map<String, String> instructionInputs,
			Map<String, Object> previousOutputs) throws IOException {

		Map<String, Object> arguments = new HashMap<>();
		List<String> missingFields = new ArrayList<>();

		log.debug("Building arguments for action '{}' with {} required fields", actionName, requiredFields.size());

		Map<String, PythonFunctionMetadata.ParameterInfo> parameters = metadata.getParameters();

		// First, process all required fields
		for (String fieldName : requiredFields.keySet()) {
			Object value = null;
			String source = null;

			// Step 1: Try exact match in action-specific inputs first (HIGHEST PRIORITY)
			if (actionInputs.containsKey(fieldName)) {
				value = actionInputs.get(fieldName);
				source = "action-specific inputs";
			}
			// Step 2: If not found, try instruction inputs
			else if (instructionInputs.containsKey(fieldName)) {
				value = instructionInputs.get(fieldName);
				source = "instruction inputs";
			}
			// Step 3: If not found, try previous outputs (LOWEST PRIORITY)
			else if (previousOutputs.containsKey(fieldName)) {
				value = previousOutputs.get(fieldName);
				source = "previous outputs";
			}

			// Populate argument if value found
			if (value != null && !value.toString().isEmpty()) {
				// Convert to proper type based on parameter metadata
				Object typedValue = convertToType(value, parameters.get(fieldName), fieldName);
				arguments.put(fieldName, typedValue);
				log.debug("Field '{}' filled from {}: {}", fieldName, source, typedValue);
			} else {
				// Mark as missing (empty string values are treated as missing)
				missingFields.add(fieldName);
				log.warn("Missing value for required field: {}", fieldName);
			}
		}

		// Second, add any additional fields from actionInputs that are not in
		// requiredFields
		for (Map.Entry<String, String> entry : actionInputs.entrySet()) {
			String fieldName = entry.getKey();
			String value = entry.getValue();

			// Skip if already added as a required field
			if (arguments.containsKey(fieldName)) {
				continue;
			}

			// Add non-required field from actionInputs if it has a value
			if (value != null && !value.isEmpty()) {
				// Convert to proper type if parameter info available
				Object typedValue = convertToType(value, parameters.get(fieldName), fieldName);
				arguments.put(fieldName, typedValue);
				log.debug("Added non-required field '{}' from action-specific inputs: {}", fieldName, typedValue);
			}
		}

		// Check if all required fields are satisfied
		if (!missingFields.isEmpty()) {
			String errorMsg = String.format(
					"Action '%s' is missing required fields: %s.\n" + "Available action-specific inputs: %s\n"
							+ "Available instruction inputs: %s\n" + "Available previous output fields: %s",
					actionName, missingFields, actionInputs.keySet(), instructionInputs.keySet(),
					previousOutputs.keySet());
			log.error(errorMsg);
			// throw new IOException(errorMsg);
		}

		log.debug("Successfully built {} arguments for action '{}'", arguments.size(), actionName);
		return arguments;
	}

	/**
	 * Convert value to the specified type based on parameter metadata
	 */
	private static Object convertToType(Object value, PythonFunctionMetadata.ParameterInfo paramInfo,
			String paramName) {
		if (value == null) {
			return null;
		}

		// If parameter info is not available, return as-is
		if (paramInfo == null) {
			return value;
		}

		String strValue = value.toString();
		String type = paramInfo.getType();

		if (type == null) {
			return strValue;
		}

		try {
			switch (type) {
			case "string":
				return strValue;

			case "number":
			case "integer":
				if (strValue.contains(".")) {
					return Double.parseDouble(strValue);
				} else {
					return Integer.parseInt(strValue);
				}

			case "boolean":
				return Boolean.parseBoolean(strValue);

			case "object":
			case "array":
				// Try to parse as JSON
				try {
					return objectMapper.readValue(strValue, Object.class);
				} catch (Exception e) {
					log.warn("Failed to parse '{}' as JSON for parameter '{}', using string value", strValue,
							paramName);
					return strValue;
				}

			default:
				return strValue;
			}
		} catch (NumberFormatException e) {
			log.warn("Failed to convert parameter '{}' to type '{}', using string value", paramName, type);
			return strValue;
		}
	}
}
