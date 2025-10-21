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
	 * Detailed result of edge merge operation
	 */
	public static class EdgeMergeResult {
		private final List<TaskDto.EdgeDto> edges;
		private final int originalCount;
		private final int exactDuplicatesRemoved;
		private final int sameFromToMerged;
		private final int redundantConnectionsRemoved;
		private final int finalCount;

		public EdgeMergeResult(List<TaskDto.EdgeDto> edges, int originalCount, int exactDuplicatesRemoved,
				int sameFromToMerged, int redundantConnectionsRemoved) {
			this.edges = edges;
			this.originalCount = originalCount;
			this.exactDuplicatesRemoved = exactDuplicatesRemoved;
			this.sameFromToMerged = sameFromToMerged;
			this.redundantConnectionsRemoved = redundantConnectionsRemoved;
			this.finalCount = edges.size();
		}

		public List<TaskDto.EdgeDto> getEdges() {
			return edges;
		}

		public int getOriginalCount() {
			return originalCount;
		}

		public int getExactDuplicatesRemoved() {
			return exactDuplicatesRemoved;
		}

		public int getSameFromToMerged() {
			return sameFromToMerged;
		}

		public int getRedundantConnectionsRemoved() {
			return redundantConnectionsRemoved;
		}

		public int getFinalCount() {
			return finalCount;
		}

		public int getTotalRemoved() {
			return originalCount - finalCount;
		}
	}

	/**
	 * Result of same from/to merge operation
	 */
	public static class SameFromToMergeResult {
		private final List<TaskDto.EdgeDto> edges;
		private final int mergeOperationsPerformed;

		public SameFromToMergeResult(List<TaskDto.EdgeDto> edges, int mergeOperationsPerformed) {
			this.edges = edges;
			this.mergeOperationsPerformed = mergeOperationsPerformed;
		}

		public List<TaskDto.EdgeDto> getEdges() {
			return edges;
		}

		public int getMergeOperationsPerformed() {
			return mergeOperationsPerformed;
		}
	}

	public static class RedundantRemovalResult {
		private final List<TaskDto.EdgeDto> edges;
		private final int redundantConnectionsRemoved;

		public RedundantRemovalResult(List<TaskDto.EdgeDto> edges, int redundantConnectionsRemoved) {
			this.edges = edges;
			this.redundantConnectionsRemoved = redundantConnectionsRemoved;
		}

		public List<TaskDto.EdgeDto> getEdges() {
			return edges;
		}

		public int getRedundantConnectionsRemoved() {
			return redundantConnectionsRemoved;
		}
	}

	/**
	 * Merge and deduplicate edges with detailed statistics (backward compatibility)
	 * 
	 * @param edges List of edges to merge
	 * @return Detailed merge result with statistics
	 * @deprecated Use mergeAndDeduplicateEdgesDetailed(edges, utility) with proper EdgeGeneratorUtility
	 */
	@Deprecated
	public EdgeMergeResult mergeAndDeduplicateEdgesDetailed(List<TaskDto.EdgeDto> edges) {
		throw new UnsupportedOperationException(
			"Environment parameters are required. Use mergeAndDeduplicateEdgesDetailed(edges, utility) instead."
		);
	}

	/**
	 * Merge and deduplicate edges with instruction prioritization. This is the main
	 * entry point for edge merging logic.
	 * 
	 * @param edges   List of edges to merge
	 * @param utility EdgeGeneratorUtility instance for field operations
	 * @return List of merged and deduplicated edges
	 */
	public List<TaskDto.EdgeDto> mergeAndDeduplicateEdges(List<TaskDto.EdgeDto> edges, EdgeGeneratorUtility utility) {
		EdgeMergeResult result = mergeAndDeduplicateEdgesDetailed(edges, utility);
		return result.getEdges();
	}

	/**
	 * Merge and deduplicate edges with detailed statistics
	 * 
	 * @param edges   List of edges to merge
	 * @param utility EdgeGeneratorUtility instance for field operations
	 * @return Detailed merge result with statistics
	 */
	public EdgeMergeResult mergeAndDeduplicateEdgesDetailed(List<TaskDto.EdgeDto> edges, EdgeGeneratorUtility utility) {
		if (edges == null || edges.isEmpty()) {
			return new EdgeMergeResult(new ArrayList<>(), 0, 0, 0, 0);
		}

		int originalCount = edges.size();

		// Step 1: Remove exact duplicate edges
		List<TaskDto.EdgeDto> deduplicatedEdges = removeExactDuplicates(edges);
		int exactDuplicatesRemoved = originalCount - deduplicatedEdges.size();
		logger.debug("After deduplication: {} edges ({} exact duplicates removed)", deduplicatedEdges.size(),
				exactDuplicatesRemoved);

		// Step 2: Merge edges with same "from" and "to" values
		SameFromToMergeResult sameFromToResult = mergeEdgesWithSameFromToDetailed(deduplicatedEdges);
		List<TaskDto.EdgeDto> mergedEdges = sameFromToResult.getEdges();
		int sameFromToMerged = sameFromToResult.getMergeOperationsPerformed();
		logger.debug("After merging same from/to: {} edges ({} merge operations performed)", mergedEdges.size(),
				sameFromToMerged);

		// Step 3: Collect instruction-provided inputs
		Map<String, List<String>> instructionProvidedInputs = collectInstructionInputs(mergedEdges);

		// Step 4: Remove redundant action->action connections
		RedundantRemovalResult redundantResult = removeRedundantActionEdgesDetailed(mergedEdges,
				instructionProvidedInputs, utility);
		List<TaskDto.EdgeDto> finalEdges = redundantResult.getEdges();
		int redundantConnectionsRemoved = redundantResult.getRedundantConnectionsRemoved();
		logger.info("After removing redundant action->action connections: {} edges ({} connections removed)",
				finalEdges.size(), redundantConnectionsRemoved);

		return new EdgeMergeResult(finalEdges, originalCount, exactDuplicatesRemoved, sameFromToMerged,
				redundantConnectionsRemoved);
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
	 * connections, with detailed tracking.
	 * 
	 * @param edges List of edges to merge
	 * @return Result with merged edges and count of merge operations
	 */
	private SameFromToMergeResult mergeEdgesWithSameFromToDetailed(List<TaskDto.EdgeDto> edges) {
		List<TaskDto.EdgeDto> mergedEdges = new ArrayList<>();
		int mergeOperationsPerformed = 0;

		for (TaskDto.EdgeDto edge : edges) {
			boolean merged = false;

			for (TaskDto.EdgeDto existingEdge : mergedEdges) {
				if (java.util.Objects.equals(edge.getFrom(), existingEdge.getFrom())
						&& java.util.Objects.equals(edge.getTo(), existingEdge.getTo())) {
					mergeConnections(existingEdge, edge);
					merged = true;
					mergeOperationsPerformed++;
					break;
				}
			}

			if (!merged) {
				mergedEdges.add(edge);
			}
		}

		return new SameFromToMergeResult(mergedEdges, mergeOperationsPerformed);
	}

	/**
	 * Merge edges that have the same "from" and "to" values by combining their
	 * connections (backward compatibility method).
	 * 
	 * @param edges List of edges to merge
	 * @return List with merged edges
	 */
	@SuppressWarnings("unused")
	private List<TaskDto.EdgeDto> mergeEdgesWithSameFromTo(List<TaskDto.EdgeDto> edges) {
		SameFromToMergeResult result = mergeEdgesWithSameFromToDetailed(edges);
		return result.getEdges();
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
	 * same input, with detailed connection tracking.
	 * 
	 * @param edges                     List of edges to filter
	 * @param instructionProvidedInputs Map of inputs provided by instruction
	 * @param utility                   EdgeGeneratorUtility instance for field
	 *                                  operations
	 * @return Result with edges and count of redundant connections removed
	 */
	private RedundantRemovalResult removeRedundantActionEdgesDetailed(List<TaskDto.EdgeDto> edges,
			Map<String, List<String>> instructionProvidedInputs, EdgeGeneratorUtility utility) {
		List<TaskDto.EdgeDto> finalEdges = new ArrayList<>();
		int redundantConnectionsRemoved = 0;

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

					int originalConnectionCount = Math.min(outputs.size(), inputs.size());
					int size = originalConnectionCount;

					for (int i = 0; i < size; i++) {
						String input = inputs.get(i);
						String output = outputs.get(i);

						// Check if this input is provided by instruction using proper field name
						// comparison
						boolean providedByInstruction = false;
						for (String instructionInput : instructionInputs) {
							if (utility.areFieldNamesEquivalent(input, instructionInput)) {
								providedByInstruction = true;
								break;
							}
						}

						// Only keep the pair if NOT provided by instruction
						if (!providedByInstruction) {
							filteredOutputs.add(output);
							filteredInputs.add(input);
						} else {
							// Count this as a redundant connection removed
							redundantConnectionsRemoved++;
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

		return new RedundantRemovalResult(finalEdges, redundantConnectionsRemoved);
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
