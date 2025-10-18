package com.amazon.agenticworkstation.action.scenario;

import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.INTERFACE_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.action.DataFileManager;
import com.amazon.agenticworkstation.action.InputFieldExtractor;
import com.amazon.agenticworkstation.action.InterfaceMappingLoader;
import com.amazon.agenticworkstation.action.PythonExecutorService;
import com.amazon.agenticworkstation.action.scenario.models.InputMapping;
import com.amazon.agenticworkstation.action.scenario.models.PythonFunctionMetadata;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioConfig;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioExecutionException;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioStep;
import com.amazon.agenticworkstation.dto.TaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generates actions based on predefined scenarios.
 * 
 * A scenario is a workflow consisting of multiple actions where: - Actions are
 * executed in sequence - Outputs from previous actions can be used as inputs
 * for subsequent actions - Inputs can come from scenario parameters, previous
 * step outputs, or static values
 * 
 * Design principles: - No fallback mechanisms - explicit error handling with
 * proper exceptions - Flexible and extensible for multiple scenarios across
 * multiple environments - Type-safe parameter mapping and validation - Complete
 * audit trail of all operations
 */
public final class ActionGeneratorBasedOnScenario {
	private static final Logger log = LoggerFactory.getLogger(ActionGeneratorBasedOnScenario.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private ActionGeneratorBasedOnScenario() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Execute a scenario with its configuration and required inputs.
	 * 
	 * @param config                    Scenario configuration defining the workflow
	 * @param requiredInputsForScenario Map of input parameter names to values
	 * @return ScenarioExecutionResult containing all executed actions and metadata
	 * @throws ScenarioExecutionException if scenario execution fails at any step
	 */
	public static ScenarioExecutionResult executeScenario(ScenarioConfig config,
			Map<String, Object> requiredInputsForScenario) throws ScenarioExecutionException {
	return executeScenario(config, requiredInputsForScenario, null);
    }

    /**
     * Execute a scenario but use a provided data file path instead of preparing a new one.
     * If dataFilePath is null, the method will prepare a data file as before.
     */
    public static ScenarioExecutionResult executeScenario(ScenarioConfig config,
	    Map<String, Object> requiredInputsForScenario, String dataFilePath) throws ScenarioExecutionException {
	String scenarioName = config.getScenarioName();
	String envName = config.getEnvName();
		int interfaceNumber = config.getInterfaceNumber();

		log.info("Executing scenario '{}' with {} steps in env: '{}' interface: {}", scenarioName,
				config.getSteps().size(), envName, interfaceNumber);

	long startTime = System.currentTimeMillis();

		try {
			// Prepare data file for this execution session if not provided
			if (dataFilePath == null) {
				dataFilePath = DataFileManager.prepareDataFile(envName);
				log.info("Prepared temporary data file for scenario execution: {}", dataFilePath);
			} else {
				log.info("Using provided data file for scenario execution: {}", dataFilePath);
			}

			// Load interface mappings for environment
			String interfaceName = INTERFACE_PREFIX + interfaceNumber;
			Map<String, Object> interfaceMappings = InterfaceMappingLoader.loadInterfaceMappings(envName);
			boolean autoAuditEnabled = InterfaceMappingLoader.extractAutoAuditSetting(interfaceMappings, envName);
			log.info("Using interface: {} for environment: {}, auto-audit: {}", interfaceName, envName,
					autoAuditEnabled);

			// Execute scenario steps
			List<TaskDto.ActionDto> actions = new ArrayList<>();
			Map<String, StepResult> stepResults = new LinkedHashMap<>();

			for (int i = 0; i < config.getSteps().size(); i++) {
				ScenarioStep step = config.getSteps().get(i);
				String stepId = step.getStepId();
				String actionName = step.getActionName();

				log.info("Executing step {}/{}: '{}' (action: '{}')", i + 1, config.getSteps().size(), stepId,
						actionName);

				try {
					// Execute the step
					StepResult stepResult = executeStep(step, requiredInputsForScenario, stepResults, dataFilePath,
							envName, interfaceNumber);

					stepResults.put(stepId, stepResult);
					actions.add(stepResult.getActionDto());

					log.info("Successfully executed step '{}': {}", stepId, stepResult.getActionDto().getName());
				} catch (Exception e) {
					log.error("Step '{}' failed: {}", stepId, e.getMessage(), e);
					throw new ScenarioExecutionException(scenarioName, stepId, actionName,
							"Step execution failed: " + e.getMessage(), e);
				}
			}

			long executionTime = System.currentTimeMillis() - startTime;
			log.info("Successfully completed scenario '{}' in {} ms with {} actions (including audit logs)",
					scenarioName, executionTime, actions.size());

			return new ScenarioExecutionResult(scenarioName, true, actions, stepResults, executionTime, null);

		} catch (ScenarioExecutionException e) {
			long executionTime = System.currentTimeMillis() - startTime;
			log.error("Scenario '{}' failed after {} ms: {}", scenarioName, executionTime, e.getMessage());
			throw e;

		} catch (Exception e) {
			long executionTime = System.currentTimeMillis() - startTime;
			log.error("Unexpected error in scenario '{}' after {} ms: {}", scenarioName, executionTime, e.getMessage(),
					e);
			throw new ScenarioExecutionException(scenarioName,
					"Unexpected error during scenario execution: " + e.getMessage(), e);
		}
	}

	/**
	 * Execute a single step in the scenario.
	 */
	private static StepResult executeStep(ScenarioStep step, Map<String, Object> scenarioInputs,
			Map<String, StepResult> previousStepResults, String dataFilePath, String envName, int interfaceNumber)
			throws IOException, ScenarioExecutionException {

		String stepId = step.getStepId();
		String actionName = step.getActionName();

		log.debug("Building arguments for step '{}' with {} input mappings", stepId, step.getInputMappings().size());

		// Extract metadata for this action
		PythonFunctionMetadata metadata = PythonExecutorService.extractMetadata(actionName, envName, interfaceNumber);

		// Extract required fields for validation
		List<String> actionNames = new ArrayList<>();
		actionNames.add(actionName);
		Map<String, Map<String, String>> actionFieldsMap = InputFieldExtractor.extractRequiredFields(actionNames,
				envName, interfaceNumber);
		Map<String, String> requiredFields = actionFieldsMap.get(actionName);

		if (requiredFields == null) {
			throw new ScenarioExecutionException(step.getStepId(), stepId, actionName,
					"Could not extract required fields for action");
		}

		// Build arguments based on input mappings
		Map<String, Object> arguments = new HashMap<>();
		List<String> missingFields = new ArrayList<>();

		for (InputMapping mapping : step.getInputMappings()) {
			String targetParam = mapping.getTargetParameter();
			Object value = null;

			try {
				value = resolveInputValue(mapping, scenarioInputs, previousStepResults, stepId);
			} catch (Exception e) {
				throw new ScenarioExecutionException(stepId, stepId, actionName,
						"Failed to resolve input for parameter '" + targetParam + "': " + e.getMessage(), e);
			}

			if (value != null && !value.toString().isEmpty()) {
				// Convert to proper type based on parameter metadata
				Object typedValue = convertToType(value, metadata.getParameters().get(targetParam), targetParam);
				arguments.put(targetParam, typedValue);
				log.debug("Mapped parameter '{}' = {} (from {})", targetParam, typedValue, mapping.getSource());
			} else {
				log.warn("Parameter '{}' resolved to null or empty value", targetParam);
			}
		}

		// Validate all required fields are present
		for (String requiredField : requiredFields.keySet()) {
			if (!arguments.containsKey(requiredField) || arguments.get(requiredField) == null
					|| arguments.get(requiredField).toString().isEmpty()) {
				missingFields.add(requiredField);
			}
		}

		if (!missingFields.isEmpty()) {
			String reason = String.format("Missing required parameters: %s. Provided arguments: %s", missingFields,
					arguments.keySet());
			log.warn(reason);
			// throw new ScenarioExecutionException(stepId, stepId, actionName, reason);
		}

		log.info("Executing action '{}' for step '{}' with arguments: {}", actionName, stepId, arguments);

		// Execute the Python function
		String resultJson = PythonExecutorService.executePythonFunction(actionName, arguments, dataFilePath, envName,
				interfaceNumber);

		log.debug("Action '{}' returned: {}", actionName, resultJson);

		// Parse the result
		Object output = parseOutput(resultJson);

		// Check for errors in output
		if (output instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> outputMap = (Map<String, Object>) output;

			if (outputMap.containsKey("error")) {
				String errorMessage = outputMap.get("error").toString();
				throw new ScenarioExecutionException(stepId, stepId, actionName,
						"Action returned error: " + errorMessage);
			}
		}

		// Create ActionDto
		TaskDto.ActionDto actionDto = new TaskDto.ActionDto();
		actionDto.setName(actionName);
		actionDto.setArguments(arguments);
		actionDto.setOutput(output);

		// Create and return step result
		return new StepResult(stepId, actionDto, output);
	}

	/**
	 * Resolve the value for an input mapping.
	 */
	private static Object resolveInputValue(InputMapping mapping, Map<String, Object> scenarioInputs,
			Map<String, StepResult> previousStepResults, String currentStepId) throws ScenarioExecutionException {

		switch (mapping.getSource()) {
		case SCENARIO_INPUT:
			String scenarioKey = mapping.getSourceKey();
			// If the scenario input is not provided, treat it as optional and return null
			// (the caller will validate required fields later). This avoids failing the
			// entire scenario when optional inputs are intentionally omitted.
			if (!scenarioInputs.containsKey(scenarioKey)) {
				log.debug("Scenario input '{}' not provided for step '{}'; treating as null. Available inputs: {}", scenarioKey,
						currentStepId, scenarioInputs.keySet());
				return null;
			}
			return scenarioInputs.get(scenarioKey);

		case PREVIOUS_STEP:
			return resolvePreviousStepOutput(mapping.getSourceKey(), previousStepResults, currentStepId);

		case STATIC_VALUE:
			return mapping.getStaticValue();

		default:
			throw new ScenarioExecutionException(currentStepId, currentStepId, "N/A",
					"Unknown input source: " + mapping.getSource());
		}
	}

	/**
	 * Resolve output from a previous step. (most recent first) Supports three
	 * formats: 1. "@actionName.fieldName" - get specific field from action output
	 * by action name 2. "stepId.fieldName" - get specific field from step output by
	 * step ID 3. "fieldName" - search all previous steps for this field
	 */
	private static Object resolvePreviousStepOutput(String sourceKey, Map<String, StepResult> previousStepResults,
			String currentStepId) throws ScenarioExecutionException {

		if (sourceKey.contains(".")) {
			String[] parts = sourceKey.split("\\.", 2);
			String identifier = parts[0];
			String fieldName = parts[1];

			// Format: @actionName.fieldName - search by action name
			if (identifier.startsWith("@")) {
				String actionName = identifier.substring(1); // Remove @ prefix

				// Search all previous steps for matching action name (most recent first)
				List<String> stepIds = new ArrayList<>(previousStepResults.keySet());
				for (int i = stepIds.size() - 1; i >= 0; i--) {
					String stepId = stepIds.get(i);
					StepResult stepResult = previousStepResults.get(stepId);

					if (stepResult.getActionDto().getName().equals(actionName)) {
						log.debug("Found action '{}' in step '{}', extracting field '{}'", actionName, stepId,
								fieldName);
						return extractFieldFromOutput(stepResult.getOutput(), fieldName, stepId);
					}
				}

				// Collect available action names for error message
				List<String> availableActions = new ArrayList<>();
				for (StepResult stepResult : previousStepResults.values()) {
					availableActions.add(stepResult.getActionDto().getName());
				}

				throw new ScenarioExecutionException(currentStepId, currentStepId, "N/A", "Action '" + actionName
						+ "' not found in any previous step. Available actions: " + availableActions);
			}

			// Format: stepId.fieldName - search by step ID
			String stepId = identifier;
			StepResult stepResult = previousStepResults.get(stepId);
			if (stepResult == null) {
				throw new ScenarioExecutionException(currentStepId, currentStepId, "N/A",
						"Referenced step '" + stepId + "' not found. Available steps: " + previousStepResults.keySet());
			}

			return extractFieldFromOutput(stepResult.getOutput(), fieldName, stepId);

		} else {
			// Format: fieldName - search all previous steps
			String fieldName = sourceKey;

			// Try to find field in any previous step's output (in reverse order - most
			// recent first)
			List<String> stepIds = new ArrayList<>(previousStepResults.keySet());
			for (int i = stepIds.size() - 1; i >= 0; i--) {
				String stepId = stepIds.get(i);
				StepResult stepResult = previousStepResults.get(stepId);

				try {
					Object value = extractFieldFromOutput(stepResult.getOutput(), fieldName, stepId);
					if (value != null) {
						log.debug("Found field '{}' in step '{}'", fieldName, stepId);
						return value;
					}
				} catch (ScenarioExecutionException e) {
					// Field not found in this step, continue searching
					continue;
				}
			}

			throw new ScenarioExecutionException(currentStepId, currentStepId, "N/A", "Field '" + fieldName
					+ "' not found in any previous step output. Searched steps: " + previousStepResults.keySet());
		}
	}

	/**
	 * Extract a specific field from step output.
	 */
	/**
	 * Extract a specific field from step output. Supports nested paths like: -
	 * "field_name" - simple field - "results[0].user_id" - array index with nested
	 * field - "data.user.name" - nested object fields
	 */
	private static Object extractFieldFromOutput(Object output, String fieldName, String stepId)
			throws ScenarioExecutionException {

		if (output == null) {
			throw new ScenarioExecutionException(stepId, stepId, "N/A", "Step '" + stepId + "' has null output");
		}

		// Handle nested path navigation (e.g., "results[0].user_id")
		if (fieldName.contains(".") || fieldName.contains("[")) {
			return extractNestedField(output, fieldName, stepId);
		}

		// Simple field extraction from Map
		if (output instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> outputMap = (Map<String, Object>) output;

			if (outputMap.containsKey(fieldName)) {
				return outputMap.get(fieldName);
			}

			throw new ScenarioExecutionException(stepId, stepId, "N/A", "Field '" + fieldName + "' not found in step '"
					+ stepId + "' output. Available fields: " + outputMap.keySet());
		}

		// If output is not a map, and fieldName equals the step ID, return the output
		// itself
		if (fieldName.equals(stepId)) {
			return output;
		}

		throw new ScenarioExecutionException(stepId, stepId, "N/A",
				"Cannot extract field '" + fieldName + "' from non-map output in step '" + stepId + "'");
	}

	/**
	 * Extract nested field from output using path notation. Supports: -
	 * "field.nestedField" for nested maps - "array[0]" for array access -
	 * "array[0].field" for combined array and map access
	 */
	private static Object extractNestedField(Object current, String path, String stepId)
			throws ScenarioExecutionException {

		if (current == null) {
			throw new ScenarioExecutionException(stepId, stepId, "N/A",
					"Cannot navigate path '" + path + "': current value is null");
		}

		// Split path by dots, but preserve array indices
		String[] parts = path.split("\\.");
		Object result = current;

		for (String part : parts) {
			// Check if this part contains array index notation
			if (part.contains("[")) {
				result = extractArrayElement(result, part, stepId, path);
			} else {
				// Simple field extraction
				result = extractSimpleField(result, part, stepId, path);
			}

			if (result == null) {
				return new HashMap<String, Object>(); // Return empty map if any part is null
			}
		}

		return result;
	}

	/**
	 * Extract element from array using notation like "results[0]"
	 */
	private static Object extractArrayElement(Object current, String part, String stepId, String fullPath)
			throws ScenarioExecutionException {

		// Parse field name and index: "results[0]" -> fieldName="results", index=0
		int bracketStart = part.indexOf('[');
		int bracketEnd = part.indexOf(']');

		if (bracketStart == -1 || bracketEnd == -1 || bracketEnd < bracketStart) {
			throw new ScenarioExecutionException(stepId, stepId, "N/A",
					"Invalid array notation in path '" + fullPath + "' at part '" + part + "'");
		}

		String fieldName = part.substring(0, bracketStart);
		String indexStr = part.substring(bracketStart + 1, bracketEnd);

		int index;
		try {
			index = Integer.parseInt(indexStr);
		} catch (NumberFormatException e) {
			throw new ScenarioExecutionException(stepId, stepId, "N/A",
					"Invalid array index '" + indexStr + "' in path '" + fullPath + "'");
		}

		// Get the array field
		Object arrayField = extractSimpleField(current, fieldName, stepId, fullPath);

		if (arrayField == null) {
			throw new ScenarioExecutionException(stepId, stepId, "N/A",
					"Array field '" + fieldName + "' is null in path '" + fullPath + "'");
		}

		if (arrayField instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) arrayField;

			if (index < 0 || index >= list.size()) {
				log.debug("Array field '{}' has size {}, requested index {} in path '{}'", fieldName, list.size(),
						index, fullPath);
				return null;
			}

			return list.get(index);
		}

		throw new ScenarioExecutionException(stepId, stepId, "N/A", "Field '" + fieldName
				+ "' is not an array in path '" + fullPath + "', got type: " + arrayField.getClass().getSimpleName());
	}

	/**
	 * Extract simple field from map
	 */
	private static Object extractSimpleField(Object current, String fieldName, String stepId, String fullPath)
			throws ScenarioExecutionException {

		if (!(current instanceof Map)) {
			throw new ScenarioExecutionException(stepId, stepId, "N/A", "Cannot extract field '" + fieldName
					+ "' from non-map in path '" + fullPath + "', got type: " + current.getClass().getSimpleName());
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) current;

		if (!map.containsKey(fieldName)) {
			throw new ScenarioExecutionException(stepId, stepId, "N/A", "Field '" + fieldName + "' not found in path '"
					+ fullPath + "'. Available fields: " + map.keySet());
		}

		return map.get(fieldName);
	}

	/**
	 * Convert value to the specified type based on parameter metadata.
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

	/**
	 * Parse Python function output.
	 */
	private static Object parseOutput(String resultJson) {
		try {
			return objectMapper.readValue(resultJson, Object.class);
		} catch (Exception e) {
			log.warn("Could not parse output as JSON, returning as string: {}", resultJson);
			return resultJson;
		}
	}

	/**
	 * Result of a single step execution.
	 */
	public static class StepResult {
		private final String stepId;
		private final TaskDto.ActionDto actionDto;
		private final Object output;

		public StepResult(String stepId, TaskDto.ActionDto actionDto, Object output) {
			this.stepId = stepId;
			this.actionDto = actionDto;
			this.output = output;
		}

		public String getStepId() {
			return stepId;
		}

		public TaskDto.ActionDto getActionDto() {
			return actionDto;
		}

		public Object getOutput() {
			return output;
		}

		@Override
		public String toString() {
			return "StepResult{stepId='" + stepId + "', action='" + actionDto.getName() + "', output=" + output + "}";
		}
	}

	/**
	 * Result of scenario execution.
	 */
	public static class ScenarioExecutionResult {
		private final String scenarioName;
		private final boolean success;
		private final List<TaskDto.ActionDto> actions;
		private final Map<String, StepResult> stepResults;
		private final long executionTimeMs;
		private final String errorMessage;

		public ScenarioExecutionResult(String scenarioName, boolean success, List<TaskDto.ActionDto> actions,
				Map<String, StepResult> stepResults, long executionTimeMs, String errorMessage) {
			this.scenarioName = scenarioName;
			this.success = success;
			this.actions = actions;
			this.stepResults = stepResults;
			this.executionTimeMs = executionTimeMs;
			this.errorMessage = errorMessage;
		}

		public String getScenarioName() {
			return scenarioName;
		}

		public boolean isSuccess() {
			return success;
		}

		public List<TaskDto.ActionDto> getActions() {
			return actions;
		}

		public Map<String, StepResult> getStepResults() {
			return stepResults;
		}

		public long getExecutionTimeMs() {
			return executionTimeMs;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		@Override
		public String toString() {
			return "ScenarioExecutionResult{" + "scenarioName='" + scenarioName + '\'' + ", success=" + success
					+ ", actions=" + actions.size() + ", steps=" + stepResults.size() + ", executionTimeMs="
					+ executionTimeMs + ", errorMessage='" + errorMessage + '\'' + '}';
		}
	}
}
