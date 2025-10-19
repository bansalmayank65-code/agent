package com.amazon.agenticworkstation.action.scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.action.scenario.ActionGeneratorBasedOnScenario.ScenarioExecutionResult;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioExecutionException;
import com.amazon.agenticworkstation.dto.TaskDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for executing multiple scenarios and merging their generated actions.
 * 
 * This service executes multiple scenarios and combines their generated actions
 * (ActionDto objects) while removing exact duplicates. This is useful for: -
 * Creating composite workflows with actual execution - Eliminating redundant
 * API calls across scenarios - Optimizing multi-scenario execution
 * 
 * Example usage:
 * 
 * <pre>
 * List&lt;ScenarioExecutionRequest&gt; requests = List.of(
 * 		new ScenarioExecutionRequest("UserProvisioningScenario", "hr_experts", 1, userParams),
 * 		new ScenarioExecutionRequest("CreateDepartmentScenario", "hr_experts", 1, deptParams));
 * 
 * ActionMergerResult result = ScenarioActionMergerService.executeAndMergeScenarios(requests);
 * List&lt;TaskDto.ActionDto&gt; combinedActions = result.getMergedActions();
 * </pre>
 */
public final class ScenarioActionMergerService {
	private static final Logger log = LoggerFactory.getLogger(ScenarioActionMergerService.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private ScenarioActionMergerService() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Flatten outputs from a scenario execution into a simple map for later
	 * lookups. Keys include: - "stepId.field" - "@actionName.field" - "field" (only
	 * if unique across steps - we keep the first occurrence)
	 */
	private static Map<String, Object> flattenScenarioOutputs(ScenarioExecutionResult result) {
		Map<String, Object> flat = new LinkedHashMap<>();

		Map<String, ActionGeneratorBasedOnScenario.StepResult> stepResults = result.getStepResults();

		for (Map.Entry<String, ActionGeneratorBasedOnScenario.StepResult> e : stepResults.entrySet()) {
			String stepId = e.getKey();
			ActionGeneratorBasedOnScenario.StepResult sr = e.getValue();
			Object output = sr.getOutput();

			// Also store the full raw output under a reserved key so callers can
			// perform nested path resolution (e.g. "step1.results[0].user_id").
			flat.putIfAbsent(stepId + ".__full", output);

			// If output is a map, add stepId.field entries
			if (output instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> outMap = (Map<String, Object>) output;
				for (Map.Entry<String, Object> fieldEntry : outMap.entrySet()) {
					String key = stepId + "." + fieldEntry.getKey();
					flat.putIfAbsent(key, fieldEntry.getValue());

					// also add by field name alone if not present
					flat.putIfAbsent(fieldEntry.getKey(), fieldEntry.getValue());
				}
			} else {
				// Non-map output: store as stepId -> value
				flat.putIfAbsent(stepId, output);
				flat.putIfAbsent(stepId + ".value", output);
			}

			// Add by action name: @actionName.field
			String actionName = sr.getActionDto().getName();
			// also store full output by action name
			flat.putIfAbsent("@" + actionName + ".__full", output);
			if (output instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> outMap = (Map<String, Object>) output;
				for (Map.Entry<String, Object> fieldEntry : outMap.entrySet()) {
					String key = "@" + actionName + "." + fieldEntry.getKey();
					flat.putIfAbsent(key, fieldEntry.getValue());
				}
			}
		}

		return flat;
	}

	/**
	 * Resolve a sourceSpec of the form "ScenarioName.rest" where rest follows the
	 * same formats supported in previous-step resolution (e.g., "stepId.field",
	 * "@action.field", or "field").
	 */
	private static Object resolveSourceSpecFromPreviousScenarios(String sourceSpec,
			Map<String, Map<String, Object>> previousScenarioFlattenedOutputs) throws ScenarioExecutionException {
		if (sourceSpec == null || sourceSpec.isEmpty()) {
			return null;
		}

		// Expect format: ScenarioName.restOfKey
		int dotIndex = sourceSpec.indexOf('.');
		if (dotIndex <= 0 || dotIndex == sourceSpec.length() - 1) {
			log.debug("Invalid sourceSpec '{}', expected 'ScenarioName.rest'", sourceSpec);
			return null;
		}

		String scenarioName = sourceSpec.substring(0, dotIndex);
		String rest = sourceSpec.substring(dotIndex + 1);

		Map<String, Object> flat = previousScenarioFlattenedOutputs.get(scenarioName);
		if (flat == null) {
			log.debug("No prior scenario outputs found for '{}'", scenarioName);
			return null;
		}

		// Direct lookup of keys like "stepId.field", "@action.field", or "field"
		if (flat.containsKey(rest)) {
			return flat.get(rest);
		}

		// If caller requested a nested path such as "step1.results[0].user_id",
		// attempt to split the rest into the step/action identifier and the
		// nested path and perform nested extraction on the full output object.
		int dotIdx = rest.indexOf('.');
		if (dotIdx > 0) {
			String first = rest.substring(0, dotIdx);
			String tail = rest.substring(dotIdx + 1);

			// Try step full output key
			String stepFullKey = first + ".__full";
			if (flat.containsKey(stepFullKey)) {
				Object full = flat.get(stepFullKey);
				try {
					return extractNestedFieldFromObject(full, tail, scenarioName);
				} catch (ScenarioExecutionException e) {
					throw new ScenarioExecutionException(scenarioName, "Failed to resolve nested mapping '" + rest
							+ "' from scenario '" + scenarioName + "': " + e.getFailureReason(), e);
				}
			}

			// Try action full output key (when first starts with '@' or not)
			String actionFullKey = (first.startsWith("@") ? first : "@" + first) + ".__full";
			if (flat.containsKey(actionFullKey)) {
				Object full = flat.get(actionFullKey);
				try {
					return extractNestedFieldFromObject(full, tail, scenarioName);
				} catch (ScenarioExecutionException e) {
					throw new ScenarioExecutionException(scenarioName, "Failed to resolve nested mapping '" + rest
							+ "' from scenario '" + scenarioName + "': " + e.getFailureReason(), e);
				}
			}
		}

		// Nothing found in direct lookups or step/action-specific nested extraction.
		// As a convenience, try resolving the nested path against any step/action
		// full outputs in this scenario. This helps when callers don't know the
		// exact step id but reference a nested path that exists somewhere.
		if (dotIdx > 0) {
			for (Map.Entry<String, Object> ent : flat.entrySet()) {
				String key = ent.getKey();
				if (key.endsWith(".__full")) {
					Object full = ent.getValue();
					try {
						Object candidate = extractNestedFieldFromObject(full, rest.substring(rest.indexOf('.') + 1),
								scenarioName);
						if (candidate != null) {
							return candidate;
						}
					} catch (ScenarioExecutionException e) {
						// ignore and continue trying other full outputs
					}
				}
			}
		}

		// Build helpful failure message with available keys to aid debugging
		StringBuilder available = new StringBuilder();
		for (String k : flat.keySet()) {
			available.append(k).append(", ");
		}
		String availableStr = available.length() > 0 ? available.substring(0, available.length() - 2) : "<none>";

		throw new ScenarioExecutionException(scenarioName, "Could not resolve mapping part '" + rest
				+ "' from scenario '" + scenarioName + "'. Available keys: " + availableStr);
	}

	/**
	 * Extract nested field from an arbitrary object using path notation (same
	 * semantics as ActionGeneratorBasedOnScenario.extractNestedField). This
	 * supports dotted navigation and array indices, e.g. "results[0].user_id".
	 */
	private static Object extractNestedFieldFromObject(Object current, String path, String scenarioName)
			throws ScenarioExecutionException {
		if (current == null) {
			throw new ScenarioExecutionException(scenarioName,
					"Cannot navigate path '" + path + "': source scenario output is null");
		}

		// If path is simple (no dots or indices) return simple extraction
		if (!path.contains(".") && !path.contains("[")) {
			// simple field extraction from map
			if (current instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) current;
				if (map.containsKey(path)) {
					return map.get(path);
				}
				throw new ScenarioExecutionException(scenarioName,
						"Field '" + path + "' not found in scenario output. Available: " + map.keySet());
			}
			// if current not map and path equals value name, return current
			if (path.equals("value")) {
				return current;
			}
			throw new ScenarioExecutionException(scenarioName,
					"Cannot extract field '" + path + "' from non-map scenario output");
		}

		// Split by dots and traverse
		String[] parts = path.split("\\.");
		Object result = current;

		for (String part : parts) {
			if (part.contains("[")) {
				result = extractArrayElementForNested(result, part, scenarioName, path);
			} else {
				result = extractSimpleFieldForNested(result, part, scenarioName, path);
			}

			if (result == null) {
				throw new ScenarioExecutionException(scenarioName,
						"Path '" + path + "' resulted in null at part '" + part + "'");
			}
		}

		return result;
	}

	private static Object extractArrayElementForNested(Object current, String part, String scenarioName,
			String fullPath) throws ScenarioExecutionException {
		int bracketStart = part.indexOf('[');
		int bracketEnd = part.indexOf(']');

		if (bracketStart == -1 || bracketEnd == -1 || bracketEnd < bracketStart) {
			throw new ScenarioExecutionException(scenarioName,
					"Invalid array notation in path '" + fullPath + "' at part '" + part + "'");
		}

		String fieldName = part.substring(0, bracketStart);
		String indexStr = part.substring(bracketStart + 1, bracketEnd);

		int index;
		try {
			index = Integer.parseInt(indexStr);
		} catch (NumberFormatException e) {
			throw new ScenarioExecutionException(scenarioName,
					"Invalid array index '" + indexStr + "' in path '" + fullPath + "'");
		}

		Object arrayField = extractSimpleFieldForNested(current, fieldName, scenarioName, fullPath);

		if (arrayField == null) {
			throw new ScenarioExecutionException(scenarioName,
					"Array field '" + fieldName + "' is null in path '" + fullPath + "'");
		}

		if (arrayField instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) arrayField;

			if (index < 0 || index >= list.size()) {
				throw new ScenarioExecutionException(scenarioName, "Array index " + index + " out of bounds (size: "
						+ list.size() + ") in path '" + fullPath + "'");
			}

			return list.get(index);
		}

		throw new ScenarioExecutionException(scenarioName,
				"Field '" + fieldName + "' is not an array in path '" + fullPath + "'");
	}

	private static Object extractSimpleFieldForNested(Object current, String fieldName, String scenarioName,
			String fullPath) throws ScenarioExecutionException {
		if (!(current instanceof Map)) {
			throw new ScenarioExecutionException(scenarioName,
					"Cannot extract field '" + fieldName + "' from non-map in path '" + fullPath + "'");
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) current;

		if (!map.containsKey(fieldName)) {
			throw new ScenarioExecutionException(scenarioName, "Field '" + fieldName + "' not found in path '"
					+ fullPath + "'. Available fields: " + map.keySet());
		}

		return map.get(fieldName);
	}

	/**
	 * Execute multiple scenarios and merge their generated actions, removing
	 * duplicates.
	 *
	 * @param requests List of scenario execution requests
	 * @return ActionMergerResult containing merged actions and execution details
	 * @throws ScenarioExecutionException if any scenario execution fails
	 */
	public static ActionMergerResult executeAndMergeScenarios(List<ScenarioExecutionRequest> requests)
			throws ScenarioExecutionException {

		if (requests == null || requests.isEmpty()) {
			throw new IllegalArgumentException("Execution requests list cannot be null or empty");
		}

		log.info("Executing and merging {} scenarios", requests.size());

		long startTime = System.currentTimeMillis();

		// Track execution results and merged actions
		List<ScenarioExecutionDetail> executionDetails = new ArrayList<>();
		List<TaskDto.ActionDto> mergedActions = new ArrayList<>();
		Map<String, ActionInfo> actionIndex = new LinkedHashMap<>(); // Key = action signature

		int totalActionsBeforeMerge = 0;
		int duplicatesRemoved = 0;

		// Track flattened outputs for previous scenarios: scenarioName -> flattenedKey
		// -> value
		Map<String, Map<String, Object>> previousScenarioFlattenedOutputs = new LinkedHashMap<>();

		// Prepare per-environment data file map when reuseDataFile is requested
		// or when multiple scenarios target the same environment (we assume
		// callers likely want a shared data file in that case).
		Map<String, String> envDataFileMap = new LinkedHashMap<>();

		// Precompute environment counts and whether any request explicitly asked for
		// reuse
		Map<String, Integer> envCounts = new LinkedHashMap<>();
		Map<String, Boolean> envExplicitReuse = new LinkedHashMap<>();
		for (ScenarioExecutionRequest r : requests) {
			envCounts.put(r.getEnvironment(), envCounts.getOrDefault(r.getEnvironment(), 0) + 1);
			if (r.isReuseDataFile()) {
				envExplicitReuse.put(r.getEnvironment(), true);
			}
		}

		// For any environment that appears more than once, or where an explicit
		// reuse was requested, prepare a shared data file now and store it.
		for (Map.Entry<String, Integer> e : envCounts.entrySet()) {
			String env = e.getKey();
			int count = e.getValue();

			if (count > 1 || envExplicitReuse.getOrDefault(env, false)) {
				try {
					String prepared = com.amazon.agenticworkstation.action.DataFileManager.prepareDataFile(env);
					envDataFileMap.put(env, prepared);
					log.info("Prepared shared data file for environment {}: {}", env, prepared);
				} catch (Exception ex) {
					// Fail fast: preparing a shared data file is a precondition
					throw new RuntimeException(
							"Failed to prepare shared data file for environment '" + env + "': " + ex.getMessage(), ex);
				}
			}
		}

		// Execute each scenario
		ActionMergerResult result = null;
		try {
			for (ScenarioExecutionRequest request : requests) {
				try {
					log.info("Executing scenario: {} (env: {}, interface: {})", request.getScenarioName(),
							request.getEnvironment(), request.getInterfaceNumber());

					// Build effective parameters for this scenario by resolving mappings from
					// previously executed scenarios
					Map<String, Object> effectiveParams = new LinkedHashMap<>();
					effectiveParams.putAll(request.getParameters());

					// Resolve explicit mappings: targetParam -> sourceSpec
					for (Map.Entry<String, String> mapping : request.getOutputToParamMappings().entrySet()) {
						String targetParam = mapping.getKey();
						String sourceSpec = mapping.getValue();

						Object resolved = resolveSourceSpecFromPreviousScenarios(sourceSpec,
								previousScenarioFlattenedOutputs);
						if (resolved != null) {
							effectiveParams.put(targetParam, resolved);
						} else {
							// Fail fast if an explicit mapping could not be resolved
							throw new ScenarioExecutionException(request.getScenarioName(),
									"Failed to resolve mapping '" + sourceSpec + "' for target parameter '"
											+ targetParam
											+ "' - ensure the source scenario and path exist and run earlier in the list");
						}
					}

					// Use pre-prepared shared data file for this environment when available.
					// If none was prepared, pass null and the scenario will prepare its own file.
					String dataFilePath = envDataFileMap.get(request.getEnvironment());

					// Execute the scenario with effective parameters; pass data file path when
					// available
					ScenarioExecutionResult executionResult = Scenarios.executeScenario(request.getScenarioName(),
							request.getEnvironment(), request.getInterfaceNumber(), effectiveParams, dataFilePath);

					log.info("Scenario '{}' executed successfully: {} actions generated in {} ms",
							request.getScenarioName(), executionResult.getActions().size(),
							executionResult.getExecutionTimeMs());

					// After successful execution, flatten outputs for this scenario for use by
					// subsequent scenarios
					Map<String, Object> flattened = flattenScenarioOutputs(executionResult);
					previousScenarioFlattenedOutputs.put(request.getScenarioName(), flattened);

					// Process each generated action
					for (TaskDto.ActionDto action : executionResult.getActions()) {
						totalActionsBeforeMerge++;

						// Generate unique signature for this action
						String signature = generateActionSignature(action);

						if (actionIndex.containsKey(signature)) {
							// Duplicate found - track it
							duplicatesRemoved++;
							ActionInfo existingInfo = actionIndex.get(signature);
							existingInfo.addSourceScenario(request.getScenarioName());

							log.debug("Duplicate action found: '{}' - already exists from scenario(s): {}",
									action.getName(), existingInfo.getSourceScenarios());
						} else {
							// New unique action - add it
							mergedActions.add(action);
							ActionInfo info = new ActionInfo(action, request.getScenarioName());
							actionIndex.put(signature, info);

							log.debug("Added unique action: '{}'", action.getName());
						}
					}

					// Store execution details
					executionDetails.add(new ScenarioExecutionDetail(request.getScenarioName(),
							request.getEnvironment(), request.getInterfaceNumber(), executionResult.getActions().size(),
							executionResult.getExecutionTimeMs(), true, null));

				} catch (ScenarioExecutionException e) {
					log.error("Scenario '{}' execution failed: {}", request.getScenarioName(), e.getMessage(), e);

					// Store failure details
					executionDetails.add(new ScenarioExecutionDetail(request.getScenarioName(),
							request.getEnvironment(), request.getInterfaceNumber(), 0, 0, false, e.getMessage()));

					// Re-throw to stop execution of remaining scenarios
					throw e;
				}
			}

			long executionTime = System.currentTimeMillis() - startTime;

			log.info(
					"All scenarios executed and merged in {} ms: {} total actions -> {} unique actions ({} duplicates removed)",
					executionTime, totalActionsBeforeMerge, mergedActions.size(), duplicatesRemoved);

			result = new ActionMergerResult(executionDetails, mergedActions, totalActionsBeforeMerge, duplicatesRemoved,
					executionTime, new ArrayList<>(actionIndex.values()));
		} finally {
			// Cleanup any prepared shared data files for environments
			if (envDataFileMap != null && !envDataFileMap.isEmpty()) {
				for (Map.Entry<String, String> e : envDataFileMap.entrySet()) {
					String env = e.getKey();
					String path = e.getValue();
					try {
						if (path != null) {
							java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(path));
							log.info("Deleted shared data file for env '{}': {}", env, path);
						}
					} catch (Exception ex) {
						log.warn("Failed to delete shared data file for env '{}' at '{}': {}", env, path,
								ex.getMessage());
					}
				}
			}
		}

		return result;
	}

	/**
	 * Generate a unique signature for an action to detect exact duplicates.
	 * 
	 * A duplicate is defined as having: - Same action name - Same arguments (keys
	 * and values) - Same output (if present)
	 * 
	 * This ensures that only truly identical actions are deduplicated.
	 */
	private static String generateActionSignature(TaskDto.ActionDto action) {
		StringBuilder signature = new StringBuilder();

		// Action name
		signature.append("action:").append(action.getName()).append("|");

		// Arguments - sorted by key for consistency
		if (action.getArguments() != null && !action.getArguments().isEmpty()) {
			List<String> argSignatures = action.getArguments().entrySet().stream()
					.map(e -> String.format("%s=%s", e.getKey(), serializeValue(e.getValue()))).sorted()
					.collect(Collectors.toList());
			signature.append("args:").append(String.join(",", argSignatures));
		} else {
			signature.append("args:none");
		}

		signature.append("|");

		// Output - serialize to ensure consistent comparison
		if (action.getOutput() != null) {
			signature.append("output:").append(serializeValue(action.getOutput()));
		} else {
			signature.append("output:none");
		}

		return signature.toString();
	}

	/**
	 * Serialize a value to a consistent string representation.
	 */
	private static String serializeValue(Object value) {
		if (value == null) {
			return "null";
		}

		try {
			// Use JSON serialization for consistent representation of complex objects
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			// Fallback to toString if JSON serialization fails
			return value.toString();
		}
	}

	/**
	 * Request to execute a specific scenario.
	 */
	public static class ScenarioExecutionRequest {
		private final String scenarioName;
		private final String environment;
		private final int interfaceNumber;
		private final Map<String, Object> parameters;
		// New: explicit mappings from previous scenario outputs -> this scenario's
		// parameters
		// key = targetParameter (for this scenario), value = sourceSpec (e.g.
		// "OtherScenario.step1.field")
		private final Map<String, String> outputToParamMappings;
		// When true, the ScenarioActionMergerService will prepare one data file per
		// environment and reuse it for all scenarios in that environment for this
		// execution request list.
		private final boolean reuseDataFile;

		public ScenarioExecutionRequest(String scenarioName, String environment, int interfaceNumber,
				Map<String, Object> parameters) {
			this.scenarioName = scenarioName;
			this.environment = environment;
			this.interfaceNumber = interfaceNumber;
			this.parameters = parameters != null ? parameters : new LinkedHashMap<>();
			this.outputToParamMappings = new LinkedHashMap<>();
			this.reuseDataFile = false;
		}

		public ScenarioExecutionRequest(String scenarioName, String environment, int interfaceNumber,
				Map<String, Object> parameters, Map<String, String> outputToParamMappings) {
			this.scenarioName = scenarioName;
			this.environment = environment;
			this.interfaceNumber = interfaceNumber;
			this.parameters = parameters != null ? parameters : new LinkedHashMap<>();
			this.outputToParamMappings = outputToParamMappings != null ? outputToParamMappings : new LinkedHashMap<>();
			this.reuseDataFile = false;
		}

		public ScenarioExecutionRequest(String scenarioName, String environment, int interfaceNumber,
				Map<String, Object> parameters, Map<String, String> outputToParamMappings, boolean reuseDataFile) {
			this.scenarioName = scenarioName;
			this.environment = environment;
			this.interfaceNumber = interfaceNumber;
			this.parameters = parameters != null ? parameters : new LinkedHashMap<>();
			this.outputToParamMappings = outputToParamMappings != null ? outputToParamMappings : new LinkedHashMap<>();
			this.reuseDataFile = reuseDataFile;
		}

		public String getScenarioName() {
			return scenarioName;
		}

		public String getEnvironment() {
			return environment;
		}

		public int getInterfaceNumber() {
			return interfaceNumber;
		}

		public Map<String, Object> getParameters() {
			return new LinkedHashMap<>(parameters);
		}

		public Map<String, String> getOutputToParamMappings() {
			return new LinkedHashMap<>(outputToParamMappings);
		}

		public boolean isReuseDataFile() {
			return reuseDataFile;
		}

		@Override
		public String toString() {
			return "ScenarioExecutionRequest{" + "scenarioName='" + scenarioName + '\'' + ", environment='"
					+ environment + '\'' + ", interfaceNumber=" + interfaceNumber + ", parameters="
					+ parameters.keySet() + '}';
		}
	}

	/**
	 * Details about a single scenario execution.
	 */
	public static class ScenarioExecutionDetail {
		private final String scenarioName;
		private final String environment;
		private final int interfaceNumber;
		private final int actionsGenerated;
		private final long executionTimeMs;
		private final boolean success;
		private final String errorMessage;

		public ScenarioExecutionDetail(String scenarioName, String environment, int interfaceNumber,
				int actionsGenerated, long executionTimeMs, boolean success, String errorMessage) {
			this.scenarioName = scenarioName;
			this.environment = environment;
			this.interfaceNumber = interfaceNumber;
			this.actionsGenerated = actionsGenerated;
			this.executionTimeMs = executionTimeMs;
			this.success = success;
			this.errorMessage = errorMessage;
		}

		public String getScenarioName() {
			return scenarioName;
		}

		public String getEnvironment() {
			return environment;
		}

		public int getInterfaceNumber() {
			return interfaceNumber;
		}

		public int getActionsGenerated() {
			return actionsGenerated;
		}

		public long getExecutionTimeMs() {
			return executionTimeMs;
		}

		public boolean isSuccess() {
			return success;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		@Override
		public String toString() {
			return "ScenarioExecutionDetail{" + "scenarioName='" + scenarioName + '\'' + ", environment='" + environment
					+ '\'' + ", interfaceNumber=" + interfaceNumber + ", actionsGenerated=" + actionsGenerated
					+ ", executionTimeMs=" + executionTimeMs + ", success=" + success + ", errorMessage='"
					+ errorMessage + '\'' + '}';
		}
	}

	/**
	 * Information about an action in the merged result, including which scenarios
	 * generated it.
	 */
	public static class ActionInfo {
		private final TaskDto.ActionDto action;
		private final List<String> sourceScenarios;

		public ActionInfo(TaskDto.ActionDto action, String firstSourceScenario) {
			this.action = action;
			this.sourceScenarios = new ArrayList<>();
			this.sourceScenarios.add(firstSourceScenario);
		}

		public void addSourceScenario(String scenarioName) {
			if (!sourceScenarios.contains(scenarioName)) {
				sourceScenarios.add(scenarioName);
			}
		}

		public TaskDto.ActionDto getAction() {
			return action;
		}

		public List<String> getSourceScenarios() {
			return new ArrayList<>(sourceScenarios);
		}

		public boolean isDuplicate() {
			return sourceScenarios.size() > 1;
		}

		@Override
		public String toString() {
			return "ActionInfo{" + "actionName='" + action.getName() + '\'' + ", sourceScenarios=" + sourceScenarios
					+ ", isDuplicate=" + isDuplicate() + '}';
		}
	}

	/**
	 * Result of executing and merging multiple scenarios.
	 */
	public static class ActionMergerResult {
		private final List<ScenarioExecutionDetail> executionDetails;
		private final List<TaskDto.ActionDto> mergedActions;
		private final int totalActionsBeforeMerge;
		private final int duplicatesRemoved;
		private final long totalExecutionTimeMs;
		private final List<ActionInfo> actionDetails;

		public ActionMergerResult(List<ScenarioExecutionDetail> executionDetails, List<TaskDto.ActionDto> mergedActions,
				int totalActionsBeforeMerge, int duplicatesRemoved, long totalExecutionTimeMs,
				List<ActionInfo> actionDetails) {
			this.executionDetails = executionDetails;
			this.mergedActions = mergedActions;
			this.totalActionsBeforeMerge = totalActionsBeforeMerge;
			this.duplicatesRemoved = duplicatesRemoved;
			this.totalExecutionTimeMs = totalExecutionTimeMs;
			this.actionDetails = actionDetails;
		}

		public List<ScenarioExecutionDetail> getExecutionDetails() {
			return new ArrayList<>(executionDetails);
		}

		public List<TaskDto.ActionDto> getMergedActions() {
			return new ArrayList<>(mergedActions);
		}

		public int getTotalActionsBeforeMerge() {
			return totalActionsBeforeMerge;
		}

		public int getDuplicatesRemoved() {
			return duplicatesRemoved;
		}

		public long getTotalExecutionTimeMs() {
			return totalExecutionTimeMs;
		}

		public List<ActionInfo> getActionDetails() {
			return new ArrayList<>(actionDetails);
		}

		/**
		 * Get a summary of the merge operation.
		 */
		public String getSummary() {
			long successCount = executionDetails.stream().filter(ScenarioExecutionDetail::isSuccess).count();

			return String.format(
					"Executed %d scenarios (%d successful, %d failed) in %d ms: "
							+ "%d total actions -> %d unique actions (%d duplicates removed)",
					executionDetails.size(), successCount, executionDetails.size() - successCount, totalExecutionTimeMs,
					totalActionsBeforeMerge, mergedActions.size(), duplicatesRemoved);
		}

		@Override
		public String toString() {
			return "ActionMergerResult{" + "scenarios=" + executionDetails.size() + ", mergedActions="
					+ mergedActions.size() + ", totalActionsBeforeMerge=" + totalActionsBeforeMerge
					+ ", duplicatesRemoved=" + duplicatesRemoved + ", totalExecutionTimeMs=" + totalExecutionTimeMs
					+ '}';
		}
	}
}
