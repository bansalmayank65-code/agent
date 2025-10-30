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
 * Merge algorithm: 
 * 1. Remove exact duplicate edges 
 * 2. Merge edges with same "from" and "to" values (controlled by allowMergeDifferentActionFields flag)
 * 3. Collect instruction-provided inputs 
 * 4. Remove redundant action->action connections when instruction provides same input
 * 
 * Merge Control:
 * The allowMergeDifferentActionFields flag controls whether edges from different actions
 * with different field names should be merged:
 * 
 * - When true (default): Full merging - edges are merged completely if they have the same from/to
 * - When false: Partial merging - only compatible field pairs are merged, incompatible pairs 
 *   remain as separate edges
 * 
 * Partial Merging Example:
 * Edge 1: input="field1,field2", output="result1,result2"
 * Edge 2: input="field1,field3", output="result1,result3"
 * Result: 
 * - Merged edge: input="field1,field2", output="result1,result2" (compatible pairs)
 * - Remaining edge: input="field3", output="result3" (incompatible pairs)
 * 
 * Usage example:
 * EdgeMergeService service = new EdgeMergeService();
 * service.setAllowMergeDifferentActionFields(false); // Enable partial merging
 * List<EdgeDto> merged = service.mergeAndDeduplicateEdges(edges, utility);
 */
@Service
public class EdgeMergeService {

	private static final Logger logger = LoggerFactory.getLogger(EdgeMergeService.class);
	
	/**
	 * Flag to control whether edges from different actions with different field names should be merged.
	 * When true (default), allows merging edges even if they're from different actions with different field names.
	 * When false, prevents merging when actions are different and field names don't match.
	 */
	private boolean allowMergeDifferentActionFields = false;
	
	/**
	 * Default constructor with default merge behavior (allows merging different action fields).
	 */
	public EdgeMergeService() {
		this.allowMergeDifferentActionFields = false;
	}
	
	/**
	 * Constructor that allows setting the merge behavior flag.
	 * 
	 * @param allowMergeDifferentActionFields true to allow merging different action fields, false to prevent it
	 */
	public EdgeMergeService(boolean allowMergeDifferentActionFields) {
		this.allowMergeDifferentActionFields = allowMergeDifferentActionFields;
	}
	
	/**
	 * Get the flag that controls merging behavior for different actions with different field names.
	 * 
	 * @return true if merging is allowed, false otherwise
	 */
	public boolean isAllowMergeDifferentActionFields() {
		return allowMergeDifferentActionFields;
	}
	
	/**
	 * Set the flag that controls merging behavior for different actions with different field names.
	 * 
	 * @param allowMergeDifferentActionFields true to allow merging, false to prevent it
	 */
	public void setAllowMergeDifferentActionFields(boolean allowMergeDifferentActionFields) {
		this.allowMergeDifferentActionFields = allowMergeDifferentActionFields;
	}

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
	 * Result of partial merge operation for compatible and incompatible fields
	 */
	public static class PartialMergeResult {
		private final String mergedInput;
		private final String mergedOutput;
		private final String remainingInput;
		private final String remainingOutput;
		private final boolean hasCompatibleFields;
		private final boolean hasRemainingFields;

		public PartialMergeResult(String mergedInput, String mergedOutput, String remainingInput, String remainingOutput,
				boolean hasCompatibleFields, boolean hasRemainingFields) {
			this.mergedInput = mergedInput;
			this.mergedOutput = mergedOutput;
			this.remainingInput = remainingInput;
			this.remainingOutput = remainingOutput;
			this.hasCompatibleFields = hasCompatibleFields;
			this.hasRemainingFields = hasRemainingFields;
		}

		public String getMergedInput() {
			return mergedInput;
		}

		public String getMergedOutput() {
			return mergedOutput;
		}

		public String getRemainingInput() {
			return remainingInput;
		}

		public String getRemainingOutput() {
			return remainingOutput;
		}

		public boolean hasCompatibleFields() {
			return hasCompatibleFields;
		}

		public boolean hasRemainingFields() {
			return hasRemainingFields;
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
	 * connections, with detailed tracking. Supports partial merging when allowMergeDifferentActionFields is false.
	 * 
	 * @param edges List of edges to merge
	 * @return Result with merged edges and count of merge operations
	 */
	private SameFromToMergeResult mergeEdgesWithSameFromToDetailed(List<TaskDto.EdgeDto> edges) {
		List<TaskDto.EdgeDto> mergedEdges = new ArrayList<>();
		int mergeOperationsPerformed = 0;

		for (TaskDto.EdgeDto edge : edges) {
			boolean processed = false;

			for (TaskDto.EdgeDto existingEdge : mergedEdges) {
				if (java.util.Objects.equals(edge.getFrom(), existingEdge.getFrom())
						&& java.util.Objects.equals(edge.getTo(), existingEdge.getTo())) {
					
					// Check if merging should be allowed based on the flag and field differences
					if (shouldAllowMerge(existingEdge, edge)) {
						if (allowMergeDifferentActionFields) {
							// Full merge - existing behavior
							mergeConnections(existingEdge, edge);
							processed = true;
							mergeOperationsPerformed++;
							break;
						} else {
							// Partial merge - only merge compatible fields
							PartialMergeResult partialResult = performPartialMerge(existingEdge, edge);
							if (partialResult.hasCompatibleFields()) {
								// Update existing edge with merged compatible fields
								existingEdge.getConnection().setInput(partialResult.getMergedInput());
								existingEdge.getConnection().setOutput(partialResult.getMergedOutput());
								
								// Add remaining incompatible fields as a new edge if any
								if (partialResult.hasRemainingFields()) {
									TaskDto.EdgeDto remainingEdge = createEdgeWithRemainingFields(edge, partialResult);
									mergedEdges.add(remainingEdge);
									logger.debug("Partial merge: compatible fields merged, incompatible fields preserved in separate edge from '{}' to '{}'", 
											edge.getFrom(), edge.getTo());
								} else {
									logger.debug("Full compatible merge: all fields from '{}' to '{}' were compatible", 
											edge.getFrom(), edge.getTo());
								}
								processed = true;
								mergeOperationsPerformed++;
								break;
							}
						}
					}
				}
			}

			if (!processed) {
				mergedEdges.add(edge);
			}
		}

		return new SameFromToMergeResult(mergedEdges, mergeOperationsPerformed);
	}

	/**
	 * Determine if two edges should be allowed to merge based on the class flag and field differences.
	 * When allowMergeDifferentActionFields is false, prevents merging if the edges are from different
	 * actions and have different field names.
	 * 
	 * @param existingEdge The existing edge in the merged list
	 * @param newEdge The new edge being considered for merging
	 * @return true if merging should be allowed, false otherwise
	 */
	private boolean shouldAllowMerge(TaskDto.EdgeDto existingEdge, TaskDto.EdgeDto newEdge) {
		// Always allow merging if the flag is true
		if (allowMergeDifferentActionFields) {
			return true;
		}
		
		// If flag is false, check if we're dealing with different actions
		String existingFrom = existingEdge.getFrom();
		String newFrom = newEdge.getFrom();
		
		// If both edges are from the same action, allow merging
		if (java.util.Objects.equals(existingFrom, newFrom)) {
			return true;
		}
		
		// If edges are from different actions, check if field names are compatible
		boolean compatible = areFieldNamesCompatible(existingEdge, newEdge);
		if (!compatible) {
			logger.debug("Merge prevented: edges from different actions '{}' and '{}' have incompatible field names", 
					existingFrom, newFrom);
		}
		return compatible;
	}
	
	/**
	 * Check if the field names in two edges are compatible for merging.
	 * This compares the input and output field names to determine if they have matching fields
	 * in both inputs AND outputs.
	 * 
	 * @param edge1 First edge to compare
	 * @param edge2 Second edge to compare
	 * @return true if field names are compatible (have matching fields in both inputs and outputs), false otherwise
	 */
	private boolean areFieldNamesCompatible(TaskDto.EdgeDto edge1, TaskDto.EdgeDto edge2) {
		TaskDto.ConnectionDto conn1 = edge1.getConnection();
		TaskDto.ConnectionDto conn2 = edge2.getConnection();
		
		// If either edge has no connection, consider them compatible
		if (conn1 == null || conn2 == null) {
			return true;
		}
		
		// Parse field names from both edges
		List<String> fields1Input = parseFields(conn1.getInput());
		List<String> fields1Output = parseFields(conn1.getOutput());
		List<String> fields2Input = parseFields(conn2.getInput());
		List<String> fields2Output = parseFields(conn2.getOutput());
		
		// Check for matching field names in inputs
		boolean hasMatchingInput = false;
		for (String field1 : fields1Input) {
			for (String field2 : fields2Input) {
				if (field1.equals(field2)) {
					hasMatchingInput = true;
					break;
				}
			}
			if (hasMatchingInput) break;
		}
		
		// Check for matching field names in outputs
		boolean hasMatchingOutput = false;
		for (String field1 : fields1Output) {
			for (String field2 : fields2Output) {
				if (field1.equals(field2)) {
					hasMatchingOutput = true;
					break;
				}
			}
			if (hasMatchingOutput) break;
		}
		
		// Both inputs and outputs must have matching field names
		return hasMatchingInput && hasMatchingOutput;
	}

	/**
	 * Perform partial merge of two edges, separating compatible and incompatible field pairs.
	 * 
	 * @param existingEdge The existing edge
	 * @param newEdge The new edge to merge
	 * @return PartialMergeResult containing merged and remaining field information
	 */
	private PartialMergeResult performPartialMerge(TaskDto.EdgeDto existingEdge, TaskDto.EdgeDto newEdge) {
		TaskDto.ConnectionDto existingConn = existingEdge.getConnection();
		TaskDto.ConnectionDto newConn = newEdge.getConnection();
		
		if (existingConn == null || newConn == null) {
			// If either has no connection, no partial merge possible
			return new PartialMergeResult("", "", "", "", false, false);
		}
		
		List<String> existingInputs = parseFields(existingConn.getInput());
		List<String> existingOutputs = parseFields(existingConn.getOutput());
		List<String> newInputs = parseFields(newConn.getInput());
		List<String> newOutputs = parseFields(newConn.getOutput());
		
		// Find compatible field pairs (input-output pairs that match in both edges)
		List<String> mergedInputs = new ArrayList<>(existingInputs);
		List<String> mergedOutputs = new ArrayList<>(existingOutputs);
		List<String> remainingInputs = new ArrayList<>();
		List<String> remainingOutputs = new ArrayList<>();
		
		// Check each input-output pair from the new edge
		int newSize = Math.min(newInputs.size(), newOutputs.size());
		for (int i = 0; i < newSize; i++) {
			String newInput = newInputs.get(i);
			String newOutput = newOutputs.get(i);
			
			// Check if this input-output pair is compatible (both input and output match existing pairs)
			boolean isCompatible = false;
			int existingSize = Math.min(existingInputs.size(), existingOutputs.size());
			
			for (int j = 0; j < existingSize; j++) {
				if (existingInputs.get(j).equals(newInput) && existingOutputs.get(j).equals(newOutput)) {
					isCompatible = true;
					break;
				}
			}
			
			if (!isCompatible) {
				// Check if we can add this as a new compatible pair (input matches any existing input, output matches any existing output)
				boolean inputMatches = existingInputs.contains(newInput);
				boolean outputMatches = existingOutputs.contains(newOutput);
				
				if (inputMatches && outputMatches) {
					// Add to merged if not already present
					boolean pairExists = false;
					int mergedSize = Math.min(mergedInputs.size(), mergedOutputs.size());
					for (int k = 0; k < mergedSize; k++) {
						if (mergedInputs.get(k).equals(newInput) && mergedOutputs.get(k).equals(newOutput)) {
							pairExists = true;
							break;
						}
					}
					if (!pairExists) {
						mergedInputs.add(newInput);
						mergedOutputs.add(newOutput);
					}
				} else {
					// This pair is incompatible, add to remaining
					remainingInputs.add(newInput);
					remainingOutputs.add(newOutput);
				}
			}
		}
		
		String mergedInputStr = String.join(EdgeGeneratorUtility.FIELD_DELIMITER, mergedInputs);
		String mergedOutputStr = String.join(EdgeGeneratorUtility.FIELD_DELIMITER, mergedOutputs);
		String remainingInputStr = String.join(EdgeGeneratorUtility.FIELD_DELIMITER, remainingInputs);
		String remainingOutputStr = String.join(EdgeGeneratorUtility.FIELD_DELIMITER, remainingOutputs);
		
		boolean hasCompatible = !mergedInputs.isEmpty();
		boolean hasRemaining = !remainingInputs.isEmpty();
		
		return new PartialMergeResult(mergedInputStr, mergedOutputStr, remainingInputStr, remainingOutputStr, 
				hasCompatible, hasRemaining);
	}
	
	/**
	 * Create a new edge with the remaining incompatible fields from a partial merge.
	 * 
	 * @param originalEdge The original edge to copy structure from
	 * @param partialResult The partial merge result containing remaining fields
	 * @return New edge with remaining fields
	 */
	private TaskDto.EdgeDto createEdgeWithRemainingFields(TaskDto.EdgeDto originalEdge, PartialMergeResult partialResult) {
		TaskDto.EdgeDto remainingEdge = new TaskDto.EdgeDto();
		remainingEdge.setFrom(originalEdge.getFrom());
		remainingEdge.setTo(originalEdge.getTo());
		
		TaskDto.ConnectionDto remainingConnection = new TaskDto.ConnectionDto();
		remainingConnection.setInput(partialResult.getRemainingInput());
		remainingConnection.setOutput(partialResult.getRemainingOutput());
		remainingEdge.setConnection(remainingConnection);
		
		return remainingEdge;
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
	 * This comparison is order-agnostic for JSON property ordering - it compares 
	 * the logical content regardless of how the JSON was structured.
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

		// Compare from and to (null-safe)
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

		// Compare connection output and input (null-safe)
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
