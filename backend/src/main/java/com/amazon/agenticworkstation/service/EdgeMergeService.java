package com.amazon.agenticworkstation.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazon.agenticworkstation.constants.EdgeGeneratorUtility;
import com.amazon.agenticworkstation.dto.TaskDto;

/**
 * Service for merging and deduplicating edges. Contains common logic used by
 * both EdgeGenerator and EdgeMergerController.
 * 
 * Merge algorithm: 1. Remove exact duplicate edges 2. Merge edges with same
 * "from" and "to" values 3. Collect instruction-provided inputs 4. Remove
 * redundant action->action connections when instruction provides same input
 */
@Service
public class EdgeMergeService {

	private static final Logger logger = LoggerFactory.getLogger(EdgeMergeService.class);

	/**
	 * Merge and deduplicate edges with instruction prioritization. This is the main
	 * entry point for edge merging logic.
	 * 
	 * @param edges List of edges to merge
	 * @return List of merged and deduplicated edges
	 */
	public List<TaskDto.EdgeDto> mergeAndDeduplicateEdges(List<TaskDto.EdgeDto> edges) {
		if (edges == null || edges.isEmpty()) {
			return new ArrayList<>();
		}

		// Step 1: Remove exact duplicate edges
		List<TaskDto.EdgeDto> deduplicatedEdges = removeExactDuplicates(edges);
		logger.debug("After deduplication: {} edges", deduplicatedEdges.size());

		// Step 2: Merge edges with same "from" and "to" values
		List<TaskDto.EdgeDto> mergedEdges = mergeEdgesWithSameFromTo(deduplicatedEdges);
		logger.debug("After merging same from/to: {} edges", mergedEdges.size());

		// Step 3: Collect instruction-provided inputs
		Map<String, List<String>> instructionProvidedInputs = collectInstructionInputs(mergedEdges);

		// Step 4: Remove redundant action->action connections
		List<TaskDto.EdgeDto> finalEdges = removeRedundantActionEdges(mergedEdges, instructionProvidedInputs);
		logger.debug("After removing redundant action->action edges: {} edges", finalEdges.size());

		return finalEdges;
	}

	/**
	 * Remove exact duplicate edges (same from, to, and connections).
	 * 
	 * @param edges List of edges to deduplicate
	 * @return List without exact duplicates
	 */
	private List<TaskDto.EdgeDto> removeExactDuplicates(List<TaskDto.EdgeDto> edges) {
		List<TaskDto.EdgeDto> deduplicatedEdges = new ArrayList<>();

		for (TaskDto.EdgeDto edge : edges) {
			boolean isDuplicate = false;

			for (TaskDto.EdgeDto existingEdge : deduplicatedEdges) {
				if (areEdgesExactlyEqual(edge, existingEdge)) {
					isDuplicate = true;
					break;
				}
			}

			if (!isDuplicate) {
				deduplicatedEdges.add(edge);
			}
		}

		return deduplicatedEdges;
	}

	/**
	 * Merge edges that have the same "from" and "to" values by combining their
	 * connections.
	 * 
	 * @param edges List of edges to merge
	 * @return List with merged edges
	 */
	private List<TaskDto.EdgeDto> mergeEdgesWithSameFromTo(List<TaskDto.EdgeDto> edges) {
		List<TaskDto.EdgeDto> mergedEdges = new ArrayList<>();

		for (TaskDto.EdgeDto edge : edges) {
			boolean merged = false;

			for (TaskDto.EdgeDto existingEdge : mergedEdges) {
				if (java.util.Objects.equals(edge.getFrom(), existingEdge.getFrom())
						&& java.util.Objects.equals(edge.getTo(), existingEdge.getTo())) {
					mergeConnections(existingEdge, edge);
					merged = true;
					break;
				}
			}

			if (!merged) {
				mergedEdges.add(edge);
			}
		}

		return mergedEdges;
	}

	/**
	 * Collect all inputs provided by instruction edges.
	 * 
	 * @param edges List of edges to analyze
	 * @return Map of action name -> list of inputs provided by instruction
	 */
	private Map<String, List<String>> collectInstructionInputs(List<TaskDto.EdgeDto> edges) {
		Map<String, List<String>> instructionProvidedInputs = new HashMap<>();

		for (TaskDto.EdgeDto edge : edges) {
			if (EdgeGeneratorUtility.INSTRUCTION.equals(edge.getFrom())) {
				String toAction = edge.getTo();
				TaskDto.ConnectionDto connection = edge.getConnection();
				if (connection != null && connection.getInput() != null) {
					List<String> inputs = parseFields(connection.getInput());
					instructionProvidedInputs.put(toAction, inputs);
				}
			}
		}

		return instructionProvidedInputs;
	}

	/**
	 * Remove redundant action->action edges when instruction already provides the
	 * same input.
	 * 
	 * @param edges                     List of edges to filter
	 * @param instructionProvidedInputs Map of inputs provided by instruction
	 * @return List with redundant edges removed
	 */
	private List<TaskDto.EdgeDto> removeRedundantActionEdges(List<TaskDto.EdgeDto> edges,
			Map<String, List<String>> instructionProvidedInputs) {
		List<TaskDto.EdgeDto> finalEdges = new ArrayList<>();

		for (TaskDto.EdgeDto edge : edges) {
			// Instruction edges are always added
			if (EdgeGeneratorUtility.INSTRUCTION.equals(edge.getFrom())) {
				finalEdges.add(edge);
				continue;
			}

			// For action->action edges, check for redundancy
			String toAction = edge.getTo();
			List<String> instructionInputs = instructionProvidedInputs.get(toAction);

			if (instructionInputs != null && !instructionInputs.isEmpty()) {
				TaskDto.ConnectionDto connection = edge.getConnection();
				if (connection != null) {
					List<String> outputs = parseFields(connection.getOutput());
					List<String> inputs = parseFields(connection.getInput());

					List<String> filteredOutputs = new ArrayList<>();
					List<String> filteredInputs = new ArrayList<>();

					int size = Math.min(outputs.size(), inputs.size());
					for (int i = 0; i < size; i++) {
						String input = inputs.get(i);
						String output = outputs.get(i);

						// Check if this input is provided by instruction using proper field name
						// comparison
						boolean providedByInstruction = false;
						for (String instructionInput : instructionInputs) {
							if (EdgeGeneratorUtility.areFieldNamesEquivalent(input, instructionInput)) {
								providedByInstruction = true;
								break;
							}
						}

						// Only keep the pair if NOT provided by instruction
						if (!providedByInstruction) {
							filteredOutputs.add(output);
							filteredInputs.add(input);
						}
					}

					// Only add the edge if there are remaining connections
					if (!filteredInputs.isEmpty()) {
						connection.setOutput(String.join(EdgeGeneratorUtility.FIELD_DELIMITER, filteredOutputs));
						connection.setInput(String.join(EdgeGeneratorUtility.FIELD_DELIMITER, filteredInputs));
						finalEdges.add(edge);
					}
				} else {
					// No connection, add as-is
					finalEdges.add(edge);
				}
			} else {
				// No instruction inputs for this action, add as-is
				finalEdges.add(edge);
			}
		}

		return finalEdges;
	}

	/**
	 * Check if two edges are exactly equal in all aspects.
	 * 
	 * @param edge1 First edge
	 * @param edge2 Second edge
	 * @return true if edges are exactly equal
	 */
	public boolean areEdgesExactlyEqual(TaskDto.EdgeDto edge1, TaskDto.EdgeDto edge2) {
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
	 * Merge connections from the second edge into the first edge by combining their
	 * inputs and outputs while maintaining proper input-output pairing.
	 * 
	 * @param targetEdge Target edge to merge into
	 * @param sourceEdge Source edge to merge from
	 */
	public void mergeConnections(TaskDto.EdgeDto targetEdge, TaskDto.EdgeDto sourceEdge) {
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
		targetConnection.setOutput(String.join(EdgeGeneratorUtility.FIELD_DELIMITER, mergedOutputs));
		targetConnection.setInput(String.join(EdgeGeneratorUtility.FIELD_DELIMITER, mergedInputs));
	}

	/**
	 * Parse comma-separated field string into a list of individual fields.
	 * 
	 * @param fieldString Comma-separated field string
	 * @return List of individual fields
	 */
	public List<String> parseFields(String fieldString) {
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
