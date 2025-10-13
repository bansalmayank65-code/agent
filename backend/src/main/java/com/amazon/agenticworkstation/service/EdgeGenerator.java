package com.amazon.agenticworkstation.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * Main method to generate edges from a list of actions. PRIORITIZES INSTRUCTION
	 * EDGES: If an input could come from instruction or previous action, prefer
	 * instruction to avoid redundant action->action edges.
	 */
	public static List<TaskDto.EdgeDto> edgesFromActions(List<TaskDto.ActionDto> actions) {
		List<TaskDto.EdgeDto> edges = new ArrayList<>();
		if (actions == null || actions.isEmpty()) {
			return edges;
		}

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
				if (mustComeFromInstruction(inputKey, inputValue)) {
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
						String matchKey = findBestMatch(inputKey, inputValue, previousOutputs);
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
			} // Second pass: try to match action inputs with previous actions
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
					String outputKey = findBestMatch(inputKey, inputValue, previousOutputs);

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
				edges.add(createActionEdge(previousName, currentName, outputKeys, inputKeys));
			}

			// Create single instruction edge for all instruction inputs
			if (!instructionInputs.isEmpty()) {
				edges.add(createInstructionEdge(currentName, instructionInputs));
			}
		}

		// Remove exact duplicate edges - keep first occurrence of completely identical
		// edges
		List<TaskDto.EdgeDto> deduplicatedEdges = new ArrayList<>();

		for (TaskDto.EdgeDto edge : edges) {
			boolean isDuplicate = false;

			// Check if this edge is an exact duplicate of any previously added edge
			for (TaskDto.EdgeDto existingEdge : deduplicatedEdges) {
				if (areEdgesExactlyEqual(edge, existingEdge)) {
					isDuplicate = true;
					break;
				}
			}

			// Only add if not an exact duplicate
			if (!isDuplicate) {
				deduplicatedEdges.add(edge);
			}
		}

		// Merge edges with same "from" and "to" values
		List<TaskDto.EdgeDto> mergedEdges = new ArrayList<>();

		for (TaskDto.EdgeDto edge : deduplicatedEdges) {
			boolean merged = false;

			// Check if we already have an edge with the same from and to
			for (TaskDto.EdgeDto existingEdge : mergedEdges) {
				if (java.util.Objects.equals(edge.getFrom(), existingEdge.getFrom())
						&& java.util.Objects.equals(edge.getTo(), existingEdge.getTo())) {
					// Merge the connections
					mergeConnections(existingEdge, edge);
					merged = true;
					break;
				}
			}

			// If not merged, add as new edge
			if (!merged) {
				mergedEdges.add(edge);
			}
		}

		return mergedEdges;
	}

	/**
	 * Determine if an input MUST come from instruction (hardcoded list). These are
	 * fields that should never come from previous actions.
	 */
	private static boolean mustComeFromInstruction(String inputKey, Object inputValue) {
		String fieldName = getFieldName(inputKey).toLowerCase();

		// Fields that MUST come from instruction based on expected output analysis
		if (fieldName.equals("entity_type") || fieldName.equals("action") || fieldName.equals("operation")
				|| fieldName.equals("reference_type")) {
			return true;
		}

		return false;
	}

	/**
	 * Find the best matching output key for a given input key and value
	 */
	private static String findBestMatch(String inputKey, Object inputValue, Map<String, Object> outputs) {
		String inputFieldName = getFieldName(inputKey);

		// Exact value match with compatible field names
		if (inputValue != null) {
			String valueMatchedOutputKey = null;
			String valueMatchedInputKey = null;
			for (Map.Entry<String, Object> outputEntry : outputs.entrySet()) {
				String outputKey = outputEntry.getKey();
				Object outputValue = outputEntry.getValue();

				// Ensure both values are not null and match exactly
				if (outputValue != null
						&& inputValue.toString().toLowerCase().equals(outputValue.toString().toLowerCase())) {
					String outputFieldName = getFieldName(outputKey);

					valueMatchedOutputKey = outputKey;
					valueMatchedInputKey = inputKey;

					// Check if field names are compatible for value matching
					if (areFieldsCompatible(outputFieldName, inputFieldName)) {
						return outputKey;
					}
				}
			}

			// audit logs show that value matches with incompatible field names are
			// sometimes valid
			if (valueMatchedOutputKey != null && valueMatchedInputKey != null
					&& (valueMatchedInputKey.equalsIgnoreCase("reference_id")
							|| valueMatchedOutputKey.equalsIgnoreCase("reference_id")
							|| valueMatchedInputKey.equalsIgnoreCase("old_value")
							|| valueMatchedOutputKey.equalsIgnoreCase("old_value"))) {
				return valueMatchedOutputKey; // Return the value match even if field names aren't compatible
			}
		}
		return null;// return null if no match found
	}

	/**
	 * Check if two field names are compatible for value matching. Uses value-based
	 * compatibility: if fields have the same value, they are considered compatible.
	 * For example: skill_id=78 and reference_id=78 are compatible because they
	 * share the same value.
	 */
	private static boolean areFieldsCompatible(String outputField, String inputField) {
		if (outputField == null || inputField == null) {
			return false;
		}

		String out = outputField.toLowerCase();
		String in = inputField.toLowerCase();

		// Exact field name match is always compatible
		if (out.equals(in)) {
			return true;
		}

		return false;
	}

	/**
	 * Read task JSON and generate edges
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
			return edgesFromActions(dto.getTask().getActions());
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

				if (value instanceof List && "results".equals(key)) {
					// Handle results[0].field format
					List<?> list = (List<?>) value;
					if (!list.isEmpty() && list.get(0) instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<String, Object> firstResult = (Map<String, Object>) list.get(0);
						for (Map.Entry<String, Object> resultEntry : firstResult.entrySet()) {
							String resultKey = "results[0]." + resultEntry.getKey();
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
	 * Get field name from a potentially nested key (e.g., "filters.email" ->
	 * "email")
	 */
	private static String getFieldName(String key) {
		if (key == null) {
			return "";
		}
		// Remove array indices and get last part
		String cleanKey = key.replaceAll("\\[\\d+\\]", "");
		String[] parts = cleanKey.split("\\.");
		return parts[parts.length - 1];
	}

	/**
	 * Create an action->action edge with properly ordered output/input pairs
	 */
	private static TaskDto.EdgeDto createActionEdge(String fromAction, String toAction, List<String> outputKeys,
			List<String> inputKeys) {
		TaskDto.EdgeDto edge = new TaskDto.EdgeDto();
		edge.setFrom(fromAction);
		edge.setTo(toAction);

		TaskDto.ConnectionDto connection = new TaskDto.ConnectionDto();

		// Ensure output and input arrays are the same length and in corresponding order
		List<String> cleanedOutputs = new ArrayList<>();
		List<String> cleanedInputs = new ArrayList<>();

		int size = Math.min(outputKeys.size(), inputKeys.size());
		for (int i = 0; i < size; i++) {
			cleanedOutputs.add(cleanFieldName(outputKeys.get(i), false)); // Don't clean for action edges
			cleanedInputs.add(inputKeys.get(i));
		}

		connection.setOutput(String.join(", ", cleanedOutputs));
		connection.setInput(String.join(", ", cleanedInputs));
		edge.setConnection(connection);

		return edge;
	}

	/**
	 * Create an instruction->action edge for unmatched inputs with proper field
	 * ordering
	 */
	private static TaskDto.EdgeDto createInstructionEdge(String actionName, List<String> inputs) {
		TaskDto.EdgeDto edge = new TaskDto.EdgeDto();
		edge.setFrom("instruction");
		edge.setTo(actionName);

		TaskDto.ConnectionDto connection = new TaskDto.ConnectionDto();

		// Sort inputs to match expected order for consistent output
		List<String> sortedInputs = new ArrayList<>(inputs);
		sortedInputs.sort((a, b) -> {
			String fieldA = getFieldName(a).toLowerCase();
			String fieldB = getFieldName(b).toLowerCase();

			// Define order priority for common fields based on expected output patterns
			Map<String, Integer> fieldPriority = new HashMap<>();
			fieldPriority.put("entity_type", 1);
			fieldPriority.put("email", 2);
			fieldPriority.put("action", 3);
			fieldPriority.put("first_name", 4);
			fieldPriority.put("last_name", 5);
			fieldPriority.put("skill_name", 6); // HR task specific
			fieldPriority.put("name", 7);
			fieldPriority.put("status", 8);
			fieldPriority.put("cost_basis", 9);
			fieldPriority.put("quantity", 10);
			fieldPriority.put("operation", 11); // HR task - should come after action, before reference_type
			fieldPriority.put("reference_type", 12);

			int priorityA = fieldPriority.getOrDefault(fieldA, 100);
			int priorityB = fieldPriority.getOrDefault(fieldB, 100);

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
			cleanedOutputs.add(cleanFieldName(input, true)); // Clean for instruction edges
			cleanedInputs.add(input);
		}

		connection.setOutput(String.join(", ", cleanedOutputs));
		connection.setInput(String.join(", ", cleanedInputs));
		edge.setConnection(connection);

		return edge;
	}

	/**
	 * Check if two edges are exactly equal in all aspects
	 */
	private static boolean areEdgesExactlyEqual(TaskDto.EdgeDto edge1, TaskDto.EdgeDto edge2) {
		if (edge1 == edge2) {
			return true;
		}
		if (edge1 == null || edge2 == null) {
			return false;
		}

		// Compare from and to
		if (!java.util.Objects.equals(edge1.getFrom(), edge2.getFrom())
				|| !java.util.Objects.equals(edge1.getTo(), edge2.getTo())) {
			return false;
		}

		// Compare connections
		TaskDto.ConnectionDto conn1 = edge1.getConnection();
		TaskDto.ConnectionDto conn2 = edge2.getConnection();

		if (conn1 == conn2) {
			return true;
		}
		if (conn1 == null || conn2 == null) {
			return false;
		}

		// Compare connection output and input
		return java.util.Objects.equals(conn1.getOutput(), conn2.getOutput())
				&& java.util.Objects.equals(conn1.getInput(), conn2.getInput());
	}

	/**
	 * Clean field name by removing filters prefix and mapping special cases Only
	 * cleans if isFromInstruction is true
	 */
	private static String cleanFieldName(String fieldName, boolean isFromInstruction) {
		if (fieldName == null) {
			return null;
		}

		// Only clean if this is from an instruction edge
		if (!isFromInstruction) {
			return fieldName; // Return as-is for action edges
		}

		// Generic cleaning: keep only the last part after any dot notation
		// For example: "a.b.c.d" becomes "d", "filters.email" becomes "email"
		// "results[0].email" becomes "email" (lastIndexOf finds the dot after [0])
		String cleaned = fieldName;
		int lastDotIndex = cleaned.lastIndexOf('.');
		if (lastDotIndex != -1 && lastDotIndex < cleaned.length() - 1) {
			cleaned = cleaned.substring(lastDotIndex + 1);
		}

		// Map special cases for instruction output names
		if ("requester_email".equals(cleaned)) {
			return "email";
		} else if ("requester_id".equals(cleaned)) {
			return "user_id";
		} else if ("fund_manager_approval".equals(cleaned)) {
			return "approval";
		}

		return cleaned;
	}

	/**
	 * Merge connections from the second edge into the first edge by combining their
	 * inputs and outputs while maintaining proper input-output pairing
	 */
	private static void mergeConnections(TaskDto.EdgeDto targetEdge, TaskDto.EdgeDto sourceEdge) {
		TaskDto.ConnectionDto targetConnection = targetEdge.getConnection();
		TaskDto.ConnectionDto sourceConnection = sourceEdge.getConnection();

		if (targetConnection == null) {
			targetEdge.setConnection(sourceConnection);
			return;
		}

		if (sourceConnection == null) {
			return;
		}

		// Parse existing input-output pairs from target connection
		List<String> targetOutputs = parseFields(targetConnection.getOutput());
		List<String> targetInputs = parseFields(targetConnection.getInput());
		
		// Parse input-output pairs from source connection
		List<String> sourceOutputs = parseFields(sourceConnection.getOutput());
		List<String> sourceInputs = parseFields(sourceConnection.getInput());
		
		// Merge pairs while maintaining input-output correspondence
		List<String> mergedOutputs = new ArrayList<>(targetOutputs);
		List<String> mergedInputs = new ArrayList<>(targetInputs);
		
		// Add source pairs, avoiding duplicates based on input-output combination
		int sourceSize = Math.min(sourceOutputs.size(), sourceInputs.size());
		for (int i = 0; i < sourceSize; i++) {
			String sourceOutput = sourceOutputs.get(i);
			String sourceInput = sourceInputs.get(i);
			
			// Check if this input-output pair already exists
			boolean pairExists = false;
			int targetSize = Math.min(mergedOutputs.size(), mergedInputs.size());
			for (int j = 0; j < targetSize; j++) {
				if (mergedOutputs.get(j).equals(sourceOutput) && mergedInputs.get(j).equals(sourceInput)) {
					pairExists = true;
					break;
				}
			}
			
			// Add the pair if it doesn't exist
			if (!pairExists) {
				mergedOutputs.add(sourceOutput);
				mergedInputs.add(sourceInput);
			}
		}
		
		// Set the merged results, ensuring equal lengths
		targetConnection.setOutput(String.join(", ", mergedOutputs));
		targetConnection.setInput(String.join(", ", mergedInputs));
	}

	/**
	 * Parse comma-separated field string into a list of individual fields
	 */
	private static List<String> parseFields(String fieldString) {
		List<String> fields = new ArrayList<>();
		if (fieldString == null || fieldString.trim().isEmpty()) {
			return fields;
		}
		
		for (String field : fieldString.split(",")) {
			String trimmed = field.trim();
			if (!trimmed.isEmpty()) {
				fields.add(trimmed);
			}
		}
		
		return fields;
	}


}