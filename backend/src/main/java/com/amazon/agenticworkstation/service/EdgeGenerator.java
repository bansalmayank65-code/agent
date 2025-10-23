package com.amazon.agenticworkstation.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.constants.EdgeGeneratorUtility;
import com.amazon.agenticworkstation.dto.TaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Clean and simplified edge generator that creates connections between actions
 * based on input/output relationships.
 * 
 * Algorithm: 1. For each action, examine its inputs 2. Try to match each input
 * with outputs from previous actions using: - Value matching (input value =
 * output value) - Field name matching (input field name = output field name) -
 * Semantic matching (skill_id -> reference_id, approval_valid ->
 * fund_manager_approval) 3. Create action->action edges for matched inputs 4.
 * Create instruction->action edges for unmatched inputs
 */
public final class EdgeGenerator {

	private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	private EdgeGenerator() {
	}

	/**
	 * Main method to generate edges from a TaskDto. Extracts environment parameters
	 * from the TaskDto itself.
	 * 
	 * @param taskDto Complete task DTO containing actions and environment
	 *                configuration
	 */
	public static List<TaskDto.EdgeDto> edgesFromTaskDto(TaskDto taskDto) {
		if (taskDto == null || taskDto.getTask() == null) {
			return new ArrayList<>();
		}

		// Extract environment parameters from TaskDto - no fallback to defaults
		if (taskDto.getEnv() == null || taskDto.getEnv().trim().isEmpty()) {
			throw new IllegalArgumentException("Environment name (env) is required and cannot be null or empty");
		}
		if (taskDto.getInterfaceNum() == null) {
			throw new IllegalArgumentException("Interface number (interfaceNum) is required and cannot be null");
		}

		String envName = taskDto.getEnv();
		Integer interfaceNum = taskDto.getInterfaceNum();

		return edgesFromActions(taskDto.getTask().getActions(), envName, interfaceNum);
	}

	/**
	 * Main method to generate edges from a list of actions. PRIORITIZES INSTRUCTION
	 * EDGES: If an input could come from instruction or previous action, prefer
	 * instruction to avoid redundant action->action edges.
	 * 
	 * @param actions      List of actions to process
	 * @param envname      Environment name for configuration
	 * @param interfaceNum Interface number for configuration
	 */
	public static List<TaskDto.EdgeDto> edgesFromActions(List<TaskDto.ActionDto> actions, String envname,
			Integer interfaceNum) {
		List<TaskDto.EdgeDto> edges = new ArrayList<>();
		if (actions == null || actions.isEmpty()) {
			return edges;
		}

		// Create EdgeGeneratorUtility instance with environment parameters
		EdgeGeneratorUtility edgeGeneratorUtility = new EdgeGeneratorUtility(envname, interfaceNum);

		// Create action order map for sorting
		Map<String, Integer> actionOrderMap = new HashMap<>();
		for (int idx = 0; idx < actions.size(); idx++) {
			TaskDto.ActionDto action = actions.get(idx);
			if (action != null && action.getName() != null) {
				actionOrderMap.put(action.getName(), idx);
			}
		}

		// Process each action in order
		for (int i = 0; i < actions.size(); i++) {
			TaskDto.ActionDto currentAction = actions.get(i);
			if (currentAction == null || currentAction.getName() == null) {
				continue;
			}

			String currentName = currentAction.getName();
			Map<String, Object> currentInputs = extractInputs(currentAction.getArguments());

			// Separate inputs into instruction vs action categories
			List<String> instructionInputs = new ArrayList<>();
			List<String> actionInputs = new ArrayList<>();

			// First pass: determine which inputs should come from instruction vs actions
			for (String inputKey : currentInputs.keySet()) {
				Object inputValue = currentInputs.get(inputKey);

				// Check if this field must come from instruction (hardcoded list)
				if (mustComeFromInstruction(inputKey, inputValue, edgeGeneratorUtility)) {
					instructionInputs.add(inputKey);
				} else {
					// Check if any previous action can provide this input
					boolean canComeFromAction = false;

					for (int j = 0; j < i && !canComeFromAction; j++) {
						TaskDto.ActionDto previousAction = actions.get(j);
						if (previousAction == null || previousAction.getName() == null) {
							continue;
						}
						Map<String, Object> previousOutputs = extractOutputs(previousAction.getOutput());
						String matchKey = findBestMatch(inputKey, inputValue, previousOutputs, currentAction.getName(),
								previousAction.getName(), edgeGeneratorUtility);
						if (matchKey != null) {
							canComeFromAction = true;

							// If we find a value match (Priority 1), prefer it over semantic matches
							Object outputValue = previousOutputs.get(matchKey);
							if (outputValue != null && inputValue != null
									&& inputValue.toString().equals(outputValue.toString())) {
								break; // Stop searching, we found the best match
							}
						}
					}

					if (canComeFromAction) {
						actionInputs.add(inputKey);
					} else {
						// Default to instruction if no action can provide it
						instructionInputs.add(inputKey);
					}
				}
			}

			// Second pass: try to match action inputs with previous actions
			// Use a map to maintain output-input pairs in order
			Map<String, List<String>> actionToOutputs = new HashMap<>();
			Map<String, List<String>> actionToInputs = new HashMap<>();

			for (String inputKey : new ArrayList<>(actionInputs)) {
				Object inputValue = currentInputs.get(inputKey);
				boolean matched = false;

				String bestPreviousName = null;
				String bestOutputKey = null;
				boolean foundValueMatch = false;

				// Check all previous actions for matches, prioritizing value matches
				for (int j = 0; j < i; j++) {
					TaskDto.ActionDto previousAction = actions.get(j);
					if (previousAction == null || previousAction.getName() == null) {
						continue;
					}

					String previousName = previousAction.getName();
					Map<String, Object> previousOutputs = extractOutputs(previousAction.getOutput());
					String outputKey = findBestMatch(inputKey, inputValue, previousOutputs, currentAction.getName(),
							previousAction.getName(), edgeGeneratorUtility);

					if (outputKey != null) {
						// Check if this is a value match (Priority 1)
						Object outputValue = previousOutputs.get(outputKey);
						boolean isValueMatch = (outputValue != null && inputValue != null
								&& inputValue.toString().equals(outputValue.toString()));

						if (isValueMatch) {
							// Value match takes absolute priority
							bestPreviousName = previousName;
							bestOutputKey = outputKey;
							foundValueMatch = true;
							matched = true;
							break; // Stop searching once we find a value match
						} else if (!foundValueMatch) {
							// Only consider semantic matches if no value match found yet
							bestPreviousName = previousName;
							bestOutputKey = outputKey;
							matched = true;
						}
					}
				}

				if (matched && bestPreviousName != null && bestOutputKey != null) {
					// Found a match - add to lists in the same order to maintain pairing
					actionToOutputs.computeIfAbsent(bestPreviousName, k -> new ArrayList<>()).add(bestOutputKey);
					actionToInputs.computeIfAbsent(bestPreviousName, k -> new ArrayList<>()).add(inputKey);
					actionInputs.remove(inputKey);
				} else {
					// If no action match found, move to instruction inputs
					instructionInputs.add(inputKey);
				}
			}

			// Create action->action edges with properly paired output-input relationships
			for (Map.Entry<String, List<String>> entry : actionToOutputs.entrySet()) {
				String previousName = entry.getKey();
				List<String> outputKeys = entry.getValue();
				List<String> inputKeys = actionToInputs.get(previousName);
				edges.add(createActionEdge(previousName, currentName, outputKeys, inputKeys, edgeGeneratorUtility));
			}

			// Create single instruction edge for all instruction inputs
			if (!instructionInputs.isEmpty()) {
				edges.add(createInstructionEdge(currentName, instructionInputs, edgeGeneratorUtility));
			}
		}

		// Use EdgeMergeService for deduplication and merging with instruction
		// prioritization
		EdgeMergeService edgeMergeService = new EdgeMergeService();
		return edgeMergeService.mergeAndDeduplicateEdges(edges, edgeGeneratorUtility);
	}

	/**
	 * Determine if an input MUST come from instruction (hardcoded list). These are
	 * fields that should never come from previous actions.
	 */
	private static boolean mustComeFromInstruction(String inputKey, Object inputValue, EdgeGeneratorUtility utility) {
		String fieldName = utility.getFieldName(inputKey).toLowerCase();

		// Fields that MUST come from instruction based on expected output analysis
		return EdgeGeneratorUtility.INSTRUCTION_ONLY_FIELDS.stream().anyMatch(field -> field.equals(fieldName));
	}

	/**
	 * Find the best matching output key for a given input key and value
	 */
	private static String findBestMatch(String inputKey, Object inputValue, Map<String, Object> prevOutputs,
			String currentAction, String previousAction, EdgeGeneratorUtility edgeGeneratorUtility) {
		String inputFieldName = edgeGeneratorUtility.getFieldName(inputKey);
		boolean isAuditCurrentAction = isAuditAction(currentAction);
		boolean isAuditPreviousAction = isAuditAction(previousAction);

		// Exact value match with compatible field names
		if (inputValue != null) {
			String valueMatchedOutputKey = null;
			String valueMatchedInputKey = null;
			for (Map.Entry<String, Object> outputEntry : prevOutputs.entrySet()) {
				String outputKey = outputEntry.getKey();
				Object outputValue = outputEntry.getValue();
				String outputFieldName = edgeGeneratorUtility.getFieldName(outputKey);

				// Ensure both values are not null and match exactly
				if (outputValue != null
						&& inputValue.toString().toLowerCase().equals(outputValue.toString().toLowerCase())) {

					valueMatchedOutputKey = outputKey;
					valueMatchedInputKey = inputKey;

					// Check if field names are compatible for value matching
					if (areFieldsCompatible(outputFieldName, inputFieldName, outputKey, inputKey,
							edgeGeneratorUtility)) {
						if (!(isAuditCurrentAction && isAuditPreviousAction)) {// Avoid matching audit->audit actions
							return outputKey; // *Case1: fieldName-fieldName and value-value match
						}
					}
				}
				if (isAuditCurrentAction) {
					if (outputValue != null
							&& (EdgeGeneratorUtility.FIELD_NAME.equalsIgnoreCase(inputFieldName)
									|| EdgeGeneratorUtility.FIELD_NAME.equalsIgnoreCase(outputFieldName))
							&& (inputValue.toString().toLowerCase().equals(outputFieldName.toLowerCase())
									|| outputValue.toString().toLowerCase().equals(inputFieldName.toLowerCase()))) {
						return EdgeGeneratorUtility.FIELD_NAME; // *Case3: For audit - if fieldName = field_name then
																// - value
						// (currentAction) - fieldName(Previous Actions) match and vice versa
					}
				}
			}

			// audit logs show that value matches with incompatible field names are
			// sometimes valid
			if (isAuditCurrentAction) {
				if (valueMatchedOutputKey != null && valueMatchedInputKey != null) {
					boolean isValidAuditField = false;
					for (String field : EdgeGeneratorUtility.AUDIT_VALID_FIELDS) {
						if (field.equalsIgnoreCase(valueMatchedInputKey)
								|| field.equalsIgnoreCase(valueMatchedOutputKey)) {
							isValidAuditField = true;
							break;
						}
					}
					if (isValidAuditField) {
						return valueMatchedOutputKey; // Return the value match even if field names aren't compatible
						// *Case2: action and fieldName is audit related and value-value match
					}
				}
			}
		}
		return null;// *Case4: No match found - fallback to instruction
	}

	private static boolean isAuditAction(String currentAction) {
		return currentAction != null && EdgeGeneratorUtility.AUDIT_ACTION_NAMES.contains(currentAction.toLowerCase());
	}

	/**
	 * Check if two field names are compatible for value matching. Uses value-based
	 * compatibility: if fields have the same value, they are considered compatible.
	 * For example: skill_id=78 and reference_id=78 are compatible because they
	 * share the same value.
	 */
	private static boolean areFieldsCompatible(String outputFieldCleaned, String inputFieldCleaned, String outputField,
			String inputField, EdgeGeneratorUtility edgeGeneratorUtility) {
		if (outputFieldCleaned == null || inputFieldCleaned == null) {
			return false;
		}

		String out = outputFieldCleaned.toLowerCase();
		String in = inputFieldCleaned.toLowerCase();

		// Exact field name match is always compatible
		if (out.equals(in)) {
			return true;
		}

		// Check if fields are semantically compatible based on known mappings
		String outOrg = outputField.toLowerCase();
		String inOrg = inputField.toLowerCase();

		// Flexible containsKey: allow partial key matches
		for (Map.Entry<String, String> entry : edgeGeneratorUtility.getCompatibleFieldMappings().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if ((outOrg.contains(key) || key.contains(outOrg)) && (value.contains(inOrg) || inOrg.contains(value))) {
				return true;
			}
			if ((inOrg.contains(key) || key.contains(inOrg)) && (value.contains(outOrg) || outOrg.contains(value))) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Read task JSON and generate edges, extracting environment parameters from the
	 * JSON
	 * 
	 * @param taskJsonPath Path to the task JSON file
	 */
	public static List<TaskDto.EdgeDto> edgesFromTaskJson(Path taskJsonPath) {
		if (taskJsonPath == null) {
			return new ArrayList<>();
		}
		try {
			if (!Files.exists(taskJsonPath)) {
				return new ArrayList<>();
			}
			String content = Files.readString(taskJsonPath);
			TaskDto dto = mapper.readValue(content, TaskDto.class);
			if (dto == null || dto.getTask() == null) {
				return new ArrayList<>();
			}

			// Extract environment parameters from TaskDto - no fallback to defaults
			if (dto.getEnv() == null || dto.getEnv().trim().isEmpty()) {
				throw new IllegalArgumentException("Environment name (env) is required and cannot be null or empty");
			}
			if (dto.getInterfaceNum() == null) {
				throw new IllegalArgumentException("Interface number (interfaceNum) is required and cannot be null");
			}

			String envName = dto.getEnv();
			Integer interfaceNum = dto.getInterfaceNum();

			return edgesFromActions(dto.getTask().getActions(), envName, interfaceNum);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Read task JSON and generate edges with explicit environment parameters
	 * 
	 * @param taskJsonPath Path to the task JSON file
	 * @param envname      Environment name for configuration
	 * @param interfaceNum Interface number for configuration
	 */
	public static List<TaskDto.EdgeDto> edgesFromTaskJson(Path taskJsonPath, String envname, Integer interfaceNum) {
		if (taskJsonPath == null) {
			return new ArrayList<>();
		}
		try {
			if (!Files.exists(taskJsonPath)) {
				return new ArrayList<>();
			}
			String content = Files.readString(taskJsonPath);
			TaskDto dto = mapper.readValue(content, TaskDto.class);
			if (dto == null || dto.getTask() == null) {
				return new ArrayList<>();
			}
			return edgesFromActions(dto.getTask().getActions(), envname, interfaceNum);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Extract inputs from action arguments, flattening nested structures
	 */
	private static Map<String, Object> extractInputs(Map<String, Object> arguments) {
		Map<String, Object> inputs = new HashMap<>();
		if (arguments == null) {
			return inputs;
		}

		for (Map.Entry<String, Object> entry : arguments.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			if (value instanceof Map) {
				// Handle nested structures like filters.email, holding_data.cost_basis
				@SuppressWarnings("unchecked")
				Map<String, Object> nestedMap = (Map<String, Object>) value;
				for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
					inputs.put(key + "." + nestedEntry.getKey(), nestedEntry.getValue());
				}
			} else {
				inputs.put(key, value);
			}
		}
		return inputs;
	}

	/**
	 * Extract outputs from action output, flattening nested structures
	 */
	private static Map<String, Object> extractOutputs(Object output) {
		Map<String, Object> outputs = new HashMap<>();
		if (output == null) {
			return outputs;
		}

		if (output instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) output;

			// First pass: add top-level fields (they take priority)
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				if (!(value instanceof Map) && !(value instanceof List)) {
					outputs.put(key, value);
				}
			}

			// Second pass: handle nested structures
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				if (value instanceof List
						&& (EdgeGeneratorUtility.RESULTS.equals(key) || EdgeGeneratorUtility.ENTITIES.equals(key))) {
					// Handle results[0].field format
					List<?> list = (List<?>) value;
					if (!list.isEmpty() && list.get(0) instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> firstResult = (Map<String, Object>) list.get(0);
						for (Map.Entry<String, Object> resultEntry : firstResult.entrySet()) {
							String resultKey = key + "[0]." + resultEntry.getKey();
							// Only add nested field if top-level doesn't exist
							if (!outputs.containsKey(resultEntry.getKey())) {
								outputs.put(resultKey, resultEntry.getValue());
							}
						}
					}
				} else if (value instanceof Map) {
					// Handle nested maps like skill_data.skill_id, holding_data.holding_id
					@SuppressWarnings("unchecked")
					Map<String, Object> nestedMap = (Map<String, Object>) value;
					for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
						String nestedKey = key + "." + nestedEntry.getKey();
						// Only add nested field if top-level doesn't exist
						if (!outputs.containsKey(nestedEntry.getKey())) {
							outputs.put(nestedKey, nestedEntry.getValue());
						}
					}
				}
			}
		}
		return outputs;
	}

	/**
	 * Create an action->action edge with properly ordered output/input pairs
	 */
	private static TaskDto.EdgeDto createActionEdge(String fromAction, String toAction, List<String> outputKeys,
			List<String> inputKeys, EdgeGeneratorUtility utility) {
		TaskDto.EdgeDto edge = new TaskDto.EdgeDto();
		edge.setFrom(fromAction);
		edge.setTo(toAction);

		TaskDto.ConnectionDto connection = new TaskDto.ConnectionDto();

		// Ensure output and input arrays are the same length and in corresponding order
		List<String> cleanedOutputs = new ArrayList<>();
		List<String> cleanedInputs = new ArrayList<>();

		int size = Math.min(outputKeys.size(), inputKeys.size());
		for (int i = 0; i < size; i++) {
			cleanedOutputs.add(utility.cleanFieldName(outputKeys.get(i), false)); // Don't clean for
																					// action edges
			cleanedInputs.add(inputKeys.get(i));
		}

		connection.setOutput(String.join(EdgeGeneratorUtility.FIELD_DELIMITER, cleanedOutputs));
		connection.setInput(String.join(EdgeGeneratorUtility.FIELD_DELIMITER, cleanedInputs));
		edge.setConnection(connection);

		return edge;
	}

	/**
	 * Create an instruction->action edge for unmatched inputs with proper field
	 * ordering
	 */
	private static TaskDto.EdgeDto createInstructionEdge(String actionName, List<String> inputs,
			EdgeGeneratorUtility utility) {
		TaskDto.EdgeDto edge = new TaskDto.EdgeDto();
		edge.setFrom(EdgeGeneratorUtility.INSTRUCTION);
		edge.setTo(actionName);

		TaskDto.ConnectionDto connection = new TaskDto.ConnectionDto();

		// Sort inputs to match expected order for consistent output
		List<String> sortedInputs = new ArrayList<>(inputs);
		sortedInputs.sort((a, b) -> {
			String fieldA = utility.getFieldName(a).toLowerCase();
			String fieldB = utility.getFieldName(b).toLowerCase();

			int priorityA = EdgeGeneratorUtility.FIELD_PRIORITY.getOrDefault(fieldA,
					EdgeGeneratorUtility.DEFAULT_FIELD_PRIORITY);
			int priorityB = EdgeGeneratorUtility.FIELD_PRIORITY.getOrDefault(fieldB,
					EdgeGeneratorUtility.DEFAULT_FIELD_PRIORITY);

			if (priorityA != priorityB) {
				return Integer.compare(priorityA, priorityB);
			}

			// If same priority, sort alphabetically
			return fieldA.compareTo(fieldB);
		});

		// For instruction edges, output and input should be in the same order
		List<String> cleanedOutputs = new ArrayList<>();
		List<String> cleanedInputs = new ArrayList<>();

		for (String input : sortedInputs) {
			cleanedOutputs.add(utility.cleanFieldName(input, true)); // Clean for instruction edges
			cleanedInputs.add(input);
		}

		connection.setOutput(String.join(EdgeGeneratorUtility.FIELD_DELIMITER, cleanedOutputs));
		connection.setInput(String.join(EdgeGeneratorUtility.FIELD_DELIMITER, cleanedInputs));
		edge.setConnection(connection);

		return edge;
	}

}
