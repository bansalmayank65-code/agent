package com.amazon.agenticworkstation.test;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.amazon.agenticworkstation.dto.TaskDto;
import com.amazon.agenticworkstation.service.EdgeGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Simple file-based validator using a main method (no JUnit annotations).
 *
 * Place task JSON files under the module folder `test/tasks_files` and run this
 * class. For each task.json the runner will: - parse the TaskDto - extract
 * expected edges from task.task.edges - invoke
 * EdgeGenerator.edgesFromTaskJson(file) - compare expected vs actual edges
 * using JSON structural equality
 *
 * Exit code: 0 for success, 1 for any mismatch or error.
 */
public class EdgeGeneratorFileTester {

	/**
	 * Optional override for the tasks directory. If empty, the default is
	 * <project-root>/test/tasks_files. You can set an absolute path or a path
	 * relative to the project working directory.
	 */
	public static String TEST_TASKS_DIR = "";

	public static void main(String[] args) {
		// Test specific case first
		if (args.length == 1 && args[0].equals("test-split")) {
			testSplitEdgeCase();
			return;
		}
		
		// Test many-to-many case
		if (args.length == 1 && args[0].equals("test-many-to-many")) {
			testManyToManyEdgeCase();
			return;
		}
		
		ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
		Path cwd = Paths.get(System.getProperty("user.dir"));
		Path tasksDir;
		if (TEST_TASKS_DIR != null && !TEST_TASKS_DIR.isBlank()) {
			Path configured = Paths.get(TEST_TASKS_DIR);
			// If configured path is relative, resolve against the current working dir
			tasksDir = configured.isAbsolute() ? configured : cwd.resolve(configured);
		} else {
			tasksDir = cwd.resolve("test").resolve("tasks_files");
		}
		System.out.println("Looking for task files in: " + tasksDir.toAbsolutePath());

		if (!Files.exists(tasksDir) || !Files.isDirectory(tasksDir)) {
			System.err.println("Directory not found: " + tasksDir.toAbsolutePath());
			System.exit(1);
		}

		boolean anyFailure = false;
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(tasksDir, "*.json")) {
			for (Path p : ds) {
				System.out.println("\n--- Validating: " + p.getFileName() + " ---");
				try {
					String content = Files.readString(p);
					TaskDto dto = mapper.readValue(content, TaskDto.class);
					List<TaskDto.EdgeDto> expected = new ArrayList<>();
					if (dto != null && dto.getTask() != null && dto.getTask().getEdges() != null) {
						expected = dto.getTask().getEdges();
					}

					List<TaskDto.EdgeDto> actual = EdgeGenerator.edgesFromTaskJson(p);

					// First try flexible comparison that allows field reordering within connections
					boolean flexibleMatch = compareEdgesFlexibly(expected, actual);
					
					JsonNode expectedNode = mapper.valueToTree(expected);
					JsonNode actualNode = mapper.valueToTree(actual);

					if (!flexibleMatch && !expectedNode.equals(actualNode)) {
						// counts
						int expectedCount = expected.size();
						int actualCount = actual.size();

						// sequence equality (order-sensitive)
						List<String> expectedJsons = toJsonStrings(mapper, expected);
						List<String> actualJsons = toJsonStrings(mapper, actual);
						boolean sequenceEqual = expectedJsons.equals(actualJsons);
						boolean multisetEqual = multisetsEqual(expectedJsons, actualJsons);

						System.err.println("MISMATCH for: " + p.getFileName());
						System.err.println("Expected edge count: " + expectedCount + ", Actual edge count: " + actualCount);

						if (expectedCount != actualCount) {
							System.err.println("Counts differ.");
							
							// Show detailed edge comparison
							showDetailedEdgeComparison(expected, actual, mapper);
							anyFailure = true;
						} else if (!sequenceEqual && multisetEqual) {
							// Same edges but sequence ORDER differs - check if it's just ordering
							System.err.println("Same edges but sequence ORDER differs.");

							// Sort expected edges by target action order and check again
							if (dto != null && dto.getTask() != null && dto.getTask().getActions() != null) {
								List<TaskDto.ActionDto> actions = dto.getTask().getActions();
								Map<String, Integer> actionOrderMap = new HashMap<>();
								for (int idx = 0; idx < actions.size(); idx++) {
									TaskDto.ActionDto action = actions.get(idx);
									if (action != null && action.getName() != null) {
										actionOrderMap.put(action.getName(), idx);
									}
								}

								List<TaskDto.EdgeDto> sortedExpected = new ArrayList<>(expected);
								sortedExpected.sort((e1, e2) -> {
									int order1 = actionOrderMap.getOrDefault(e1.getTo(), Integer.MAX_VALUE);
									int order2 = actionOrderMap.getOrDefault(e2.getTo(), Integer.MAX_VALUE);
									if (order1 != order2)
										return Integer.compare(order1, order2);
									// If same target, instruction edges before action edges
									boolean e1FromInstruction = "instruction".equals(e1.getFrom());
									boolean e2FromInstruction = "instruction".equals(e2.getFrom());
									if (e1FromInstruction && !e2FromInstruction)
										return -1;
									if (!e1FromInstruction && e2FromInstruction)
										return 1;
									// If both from actions, sort by source order
									int srcOrder1 = actionOrderMap.getOrDefault(e1.getFrom(), Integer.MAX_VALUE);
									int srcOrder2 = actionOrderMap.getOrDefault(e2.getFrom(), Integer.MAX_VALUE);
									return Integer.compare(srcOrder1, srcOrder2);
								});

								JsonNode sortedExpectedNode = mapper.valueToTree(sortedExpected);
								if (sortedExpectedNode.equals(actualNode)) {
									System.out.println("✓ Expected edges match after sorting by target action order.");
									System.out.println(
											"  The edges are correct but were specified in a different order in the JSON.");
									System.out.println("  File: " + p.getFileName());
								} else {
									System.err.println(
											"✗ Expected edges still differ after sorting by target action order.");
									showDetailedEdgeComparison(expected, actual, mapper);
									anyFailure = true;
								}
							} else {
								System.err.println("✗ Cannot sort expected edges - task actions not available.");
								showDetailedEdgeComparison(expected, actual, mapper);
								anyFailure = true;
							}
						} else {
							System.err.println("Edges differ (both data and/or order differ).");
							showDetailedEdgeComparison(expected, actual, mapper);
							anyFailure = true;
						}
					} else if (flexibleMatch) {
						System.out.println("✓ OK: edges match (with flexible field ordering) for " + p.getFileName());
					} else {
						System.out.println("✓ OK: edges match for " + p.getFileName());
					}

				} catch (IOException e) {
					System.err.println("Failed to validate " + p + ": " + e.getMessage());
					anyFailure = true;
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to list task files: " + e.getMessage());
			System.exit(1);
		}

		if (anyFailure) {
			System.err.println("\nValidation finished: FAIL");
			System.exit(1);
		} else {
			System.out.println("\nValidation finished: SUCCESS");
			System.exit(0);
		}
	}

	/**
	 * Show detailed comparison of expected vs actual edges for debugging.
	 */
	private static void showDetailedEdgeComparison(List<TaskDto.EdgeDto> expected, List<TaskDto.EdgeDto> actual, ObjectMapper mapper) {
		try {
			System.err.println("\n--- EDGE DIFF ---");
			
			// Find missing and extra edges
			List<TaskDto.EdgeDto> actualCopy = new ArrayList<>(actual);
			List<TaskDto.EdgeDto> missing = new ArrayList<>();
			List<TaskDto.EdgeDto> extra = new ArrayList<>();
			
			// Find missing edges (in expected but not in actual)
			for (TaskDto.EdgeDto expectedEdge : expected) {
				boolean found = false;
				for (int j = 0; j < actualCopy.size(); j++) {
					if (edgesMatchFlexibly(expectedEdge, actualCopy.get(j))) {
						actualCopy.remove(j);
						found = true;
						break;
					}
				}
				if (!found) {
					missing.add(expectedEdge);
				}
			}
			
			// Remaining in actualCopy are extra edges
			extra.addAll(actualCopy);
			
			// Show diff format
			if (missing.isEmpty() && extra.isEmpty()) {
				System.err.println("  (no differences found)");
			} else {
				for (TaskDto.EdgeDto missingEdge : missing) {
					System.err.println("- " + formatEdge(missingEdge));
					System.err.println("  " + mapper.writeValueAsString(missingEdge));
				}
				for (TaskDto.EdgeDto extraEdge : extra) {
					System.err.println("+ " + formatEdge(extraEdge));
					System.err.println("  " + mapper.writeValueAsString(extraEdge));
				}
			}
			
			System.err.println("--- END EDGE DIFF ---\n");
			
		} catch (Exception e) {
			System.err.println("Error during edge diff: " + e.getMessage());
		}
	}
	
	/**
	 * Format an edge for readable display.
	 */
	private static String formatEdge(TaskDto.EdgeDto edge) {
		if (edge == null) {
			return "null";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(edge.getFrom()).append(" → ").append(edge.getTo());
		
		TaskDto.ConnectionDto conn = edge.getConnection();
		if (conn != null) {
			sb.append(" | ");
			String output = conn.getOutput();
			String input = conn.getInput();
			if (output != null || input != null) {
				sb.append("OUT:[").append(output != null ? output : "null").append("] ");
				sb.append("IN:[").append(input != null ? input : "null").append("]");
			}
		}
		
		return sb.toString();
	}

	/**
	 * Produce a simple line-based diff between expected and actual pretty-printed
	 * JSON. Lines present only in expected are prefixed with "- ", lines only in
	 * actual with "+ ", unchanged lines with " ". This uses an LCS algorithm to
	 * align common lines.
	 */
	private static String generateDiff(String expectedPretty, String actualPretty) {
		String[] aLines = expectedPretty.split("\r?\n");
		String[] bLines = actualPretty.split("\r?\n");

		int n = aLines.length;
		int m = bLines.length;
		int[][] dp = new int[n + 1][m + 1];

		for (int i = n - 1; i >= 0; --i) {
			for (int j = m - 1; j >= 0; --j) {
				if (aLines[i].equals(bLines[j]))
					dp[i][j] = dp[i + 1][j + 1] + 1;
				else
					dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
			}
		}

		StringBuilder out = new StringBuilder();
		int i = 0, j = 0;
		while (i < n && j < m) {
			if (aLines[i].equals(bLines[j])) {
				out.append("  ").append(aLines[i]).append(System.lineSeparator());
				i++;
				j++;
			} else if (dp[i + 1][j] >= dp[i][j + 1]) {
				out.append("- ").append(aLines[i]).append(System.lineSeparator());
				i++;
			} else {
				out.append("+ ").append(bLines[j]).append(System.lineSeparator());
				j++;
			}
		}

		while (i < n) {
			out.append("- ").append(aLines[i]).append(System.lineSeparator());
			i++;
		}
		while (j < m) {
			out.append("+ ").append(bLines[j]).append(System.lineSeparator());
			j++;
		}

		return out.toString();
	}

	private static List<String> toJsonStrings(ObjectMapper mapper, List<TaskDto.EdgeDto> list) throws IOException {
		List<String> out = new ArrayList<>();
		for (TaskDto.EdgeDto e : list) {
			out.add(mapper.writeValueAsString(e));
		}
		return out;
	}

	private static boolean multisetsEqual(List<String> a, List<String> b) {
		java.util.Map<String, Integer> cnt = new java.util.HashMap<>();
		for (String s : a)
			cnt.put(s, cnt.getOrDefault(s, 0) + 1);
		for (String s : b) {
			Integer c = cnt.get(s);
			if (c == null)
				return false;
			if (c == 1)
				cnt.remove(s);
			else
				cnt.put(s, c - 1);
		}
		return cnt.isEmpty();
	}

	/**
	 * Compare edges flexibly, allowing field reordering within input/output strings
	 * and allowing single expected edges to be matched by multiple actual edges to the same target.
	 */
	private static boolean compareEdgesFlexibly(List<TaskDto.EdgeDto> expected, List<TaskDto.EdgeDto> actual) {
		// For each expected edge, try to match it with one or more actual edges
		List<TaskDto.EdgeDto> actualCopy = new ArrayList<>(actual);
		List<TaskDto.EdgeDto> unmatchedExpected = new ArrayList<>();
		
		for (TaskDto.EdgeDto expectedEdge : expected) {
			// First try direct match
			boolean directMatch = false;
			for (int i = 0; i < actualCopy.size(); i++) {
				TaskDto.EdgeDto actualEdge = actualCopy.get(i);
				if (edgesMatchFlexibly(expectedEdge, actualEdge)) {
					actualCopy.remove(i);
					directMatch = true;
					break;
				}
			}
			
			if (!directMatch) {
				// Try to match with multiple edges to the same target (edge splitting)
				boolean multiMatch = tryMatchSplitEdge(expectedEdge, actualCopy);
				if (!multiMatch) {
					unmatchedExpected.add(expectedEdge);
				}
			}
		}
		
		// Try many-to-many matching for remaining unmatched expected edges
		if (!unmatchedExpected.isEmpty()) {
			tryMatchMultipleExpectedEdges(unmatchedExpected, actualCopy);
		}
		
		// If there are still unmatched edges, log them for debugging
		if (!unmatchedExpected.isEmpty() || !actualCopy.isEmpty()) {
			System.err.println("FLEXIBLE MATCHING FAILED:");
			try {
				ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
				
				if (!unmatchedExpected.isEmpty()) {
					System.err.println("  Unmatched expected edges (JSON):");
					for (TaskDto.EdgeDto expectedEdge : unmatchedExpected) {
						System.err.println("    " + jsonMapper.writeValueAsString(expectedEdge));
					}
				}
				if (!actualCopy.isEmpty()) {
					System.err.println("  Unmatched actual edges (JSON):");
					for (TaskDto.EdgeDto edge : actualCopy) {
						System.err.println("    " + jsonMapper.writeValueAsString(edge));
					}
				}
			} catch (Exception e) {
				System.err.println("  Error formatting JSON: " + e.getMessage());
				// Fallback to original format
				if (!unmatchedExpected.isEmpty()) {
					System.err.println("  Unmatched expected edges:");
					for (TaskDto.EdgeDto edge : unmatchedExpected) {
						System.err.println("    - " + formatEdge(edge));
					}
				}
				if (!actualCopy.isEmpty()) {
					System.err.println("  Unmatched actual edges:");
					for (TaskDto.EdgeDto edge : actualCopy) {
						System.err.println("    + " + formatEdge(edge));
					}
				}
			}
			return false;
		}
		
		return true;
	}

	/**
	 * Try to match a single expected edge with multiple actual edges to the same target.
	 * This handles cases where an expected edge like "instruction->verify_approval" with 
	 * "action, email" input is split into separate actual edges.
	 */
	/**
	 * Try to match an expected edge with a combination of actual edges that might represent the same connection split up.
	 * Key rule: "from" can be different but "to" must be same.
	 */
	private static boolean tryMatchSplitEdge(TaskDto.EdgeDto expectedEdge, List<TaskDto.EdgeDto> actualCopy) {
		String expectedTo = expectedEdge.getTo();
		if (expectedTo == null || expectedEdge.getConnection() == null) {
			return false;
		}
		
		// Find all actual edges going to the same target (from can be different, but to must be same)
		List<TaskDto.EdgeDto> candidateEdges = new ArrayList<>();
		for (TaskDto.EdgeDto actualEdge : actualCopy) {
			// CRITICAL: Only match edges with same "to" field, "from" can be different
			if (expectedTo.equals(actualEdge.getTo())) {
				candidateEdges.add(actualEdge);
			}
		}
		
		if (candidateEdges.isEmpty()) {
			return false;
		}
		
		// Parse expected input fields
		String expectedInput = expectedEdge.getConnection().getInput();
		List<String> expectedInputFields = parseFields(expectedInput);
		
		// Try to find a combination of actual edges that covers all expected input fields
		return tryMatchInputFieldsCombination(expectedInputFields, candidateEdges, actualCopy);
	}
	
	/**
	 * Try to match multiple expected edges against multiple actual edges for the same target.
	 * This handles cases where field distribution differs between expected and actual.
	 */
	private static boolean tryMatchMultipleExpectedEdges(List<TaskDto.EdgeDto> unmatchedExpected, 
														 List<TaskDto.EdgeDto> actualCopy) {
		// Group unmatched expected edges by their target
		Map<String, List<TaskDto.EdgeDto>> expectedByTarget = new HashMap<>();
		for (TaskDto.EdgeDto edge : unmatchedExpected) {
			String target = edge.getTo();
			if (target != null) {
				expectedByTarget.computeIfAbsent(target, k -> new ArrayList<>()).add(edge);
			}
		}
		
		// For each target with multiple unmatched expected edges, try to match against actual edges
		for (Map.Entry<String, List<TaskDto.EdgeDto>> entry : expectedByTarget.entrySet()) {
			String target = entry.getKey();
			List<TaskDto.EdgeDto> expectedEdgesForTarget = entry.getValue();
			
			if (expectedEdgesForTarget.size() < 2) {
				continue; // Only process targets with multiple expected edges
			}
			
			// Find actual edges for this target
			List<TaskDto.EdgeDto> actualEdgesForTarget = new ArrayList<>();
			for (TaskDto.EdgeDto actualEdge : actualCopy) {
				if (target.equals(actualEdge.getTo())) {
					actualEdgesForTarget.add(actualEdge);
				}
			}
			
			if (actualEdgesForTarget.isEmpty()) {
				continue;
			}
			
			// Try to match the group of expected edges against the group of actual edges
			if (tryMatchEdgeGroups(expectedEdgesForTarget, actualEdgesForTarget, actualCopy, unmatchedExpected)) {
				return true; // Found a match for this target
			}
		}
		
		return false;
	}
	
	/**
	 * Try to match a group of expected edges against a group of actual edges for the same target.
	 */
	private static boolean tryMatchEdgeGroups(List<TaskDto.EdgeDto> expectedGroup,
											  List<TaskDto.EdgeDto> actualGroup,
											  List<TaskDto.EdgeDto> actualCopy,
											  List<TaskDto.EdgeDto> unmatchedExpected) {
		// Collect all input fields from expected edges
		Set<String> allExpectedFields = new HashSet<>();
		for (TaskDto.EdgeDto edge : expectedGroup) {
			if (edge.getConnection() != null && edge.getConnection().getInput() != null) {
				List<String> fields = parseFields(edge.getConnection().getInput());
				for (String field : fields) {
					allExpectedFields.add(getCleanFieldName(field));
				}
			}
		}
		
		// Collect all input fields from actual edges
		Set<String> allActualFields = new HashSet<>();
		for (TaskDto.EdgeDto edge : actualGroup) {
			if (edge.getConnection() != null && edge.getConnection().getInput() != null) {
				List<String> fields = parseFields(edge.getConnection().getInput());
				for (String field : fields) {
					allActualFields.add(getCleanFieldName(field));
				}
			}
		}
		
		// Check if field sets match exactly
		if (allExpectedFields.equals(allActualFields)) {
			// Remove matched edges from both lists
			for (TaskDto.EdgeDto matchedActual : actualGroup) {
				actualCopy.remove(matchedActual);
			}
			for (TaskDto.EdgeDto matchedExpected : expectedGroup) {
				unmatchedExpected.remove(matchedExpected);
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * Try to match expected input fields with a combination of actual edges.
	 * All candidate edges must have the same "to" field, but can have different "from" fields.
	 */
	private static boolean tryMatchInputFieldsCombination(List<String> expectedInputFields, 
														  List<TaskDto.EdgeDto> candidateEdges,
														  List<TaskDto.EdgeDto> actualCopy) {
		// Create a map of actual edges and their input fields
		Map<TaskDto.EdgeDto, List<String>> edgeInputFields = new HashMap<>();
		for (TaskDto.EdgeDto edge : candidateEdges) {
			if (edge.getConnection() != null && edge.getConnection().getInput() != null) {
				List<String> inputFields = parseFields(edge.getConnection().getInput());
				edgeInputFields.put(edge, inputFields);
			}
		}
		
		// Try to find a subset of edges that collectively cover all expected input fields
		Set<TaskDto.EdgeDto> matchingEdges = new HashSet<>();
		Set<String> coveredFields = new HashSet<>();
		
		for (String expectedField : expectedInputFields) {
			boolean fieldMatched = false;
			
			// Look for any edge that provides this field (including ones already used for other fields)
			for (TaskDto.EdgeDto candidateEdge : candidateEdges) {
				List<String> inputFields = edgeInputFields.get(candidateEdge);
				if (inputFields != null && containsFieldFlexibly(inputFields, expectedField)) {
					matchingEdges.add(candidateEdge); // Set automatically handles duplicates
					fieldMatched = true;
					break; // Found a match for this field, move to next
				}
			}
			
			if (!fieldMatched) {
				return false; // Can't match this expected field
			}
		}
		
		// Now check if the matching edges collectively provide all expected fields and nothing extra
		for (TaskDto.EdgeDto edge : matchingEdges) {
			List<String> inputFields = edgeInputFields.get(edge);
			if (inputFields != null) {
				for (String field : inputFields) {
					coveredFields.add(getCleanFieldName(field));
				}
			}
		}
		
		Set<String> expectedFieldsSet = new HashSet<>();
		for (String field : expectedInputFields) {
			expectedFieldsSet.add(getCleanFieldName(field));
		}
		
		if (expectedFieldsSet.equals(coveredFields)) {
			// Remove the matched edges from actualCopy
			for (TaskDto.EdgeDto matchedEdge : matchingEdges) {
				actualCopy.remove(matchedEdge);
			}
			return true;
		}
		
		return false;
	}
	
	/**
	 * Check if a field list contains a field, allowing flexible matching (e.g., status matches status)
	 */
	private static boolean containsFieldFlexibly(List<String> fieldList, String targetField) {
		for (String field : fieldList) {
			if (fieldsMatchFlexibly(field, targetField)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Check if two field names match flexibly (same semantic meaning)
	 */
	private static boolean fieldsMatchFlexibly(String field1, String field2) {
		if (field1 == null || field2 == null) {
			return false;
		}
		
		// Extract base field names (remove prefixes like filters., results[0].)
		String cleanField1 = getCleanFieldName(field1);
		String cleanField2 = getCleanFieldName(field2);
		
		return cleanField1.equals(cleanField2);
	}
	
	/**
	 * Clean field name by removing common prefixes
	 */
	private static String getCleanFieldName(String fieldName) {
		if (fieldName == null) {
			return "";
		}
		
		// Remove array indices and get last part after dots
		String cleaned = fieldName.replaceAll("\\[\\d+\\]", "");
		int lastDotIndex = cleaned.lastIndexOf('.');
		if (lastDotIndex != -1 && lastDotIndex < cleaned.length() - 1) {
			cleaned = cleaned.substring(lastDotIndex + 1);
		}
		
		return cleaned.toLowerCase();
	}
	
	/**
	 * Check if two edges match, allowing flexible field ordering in connections.
	 */
	private static boolean edgesMatchFlexibly(TaskDto.EdgeDto expected, TaskDto.EdgeDto actual) {
		// From and To must match exactly
		if (!java.util.Objects.equals(expected.getFrom(), actual.getFrom()) ||
			!java.util.Objects.equals(expected.getTo(), actual.getTo())) {
			return false;
		}
		
		// Check connections flexibly
		TaskDto.ConnectionDto expectedConn = expected.getConnection();
		TaskDto.ConnectionDto actualConn = actual.getConnection();
		
		if (expectedConn == null && actualConn == null) {
			return true;
		}
		if (expectedConn == null || actualConn == null) {
			return false;
		}
		
		return connectionsMatchFlexibly(expectedConn, actualConn);
	}

	/**
	 * Check if two connections match, allowing reordering of fields within
	 * input and output as long as both follow the same reordering pattern.
	 */
	private static boolean connectionsMatchFlexibly(TaskDto.ConnectionDto expected, TaskDto.ConnectionDto actual) {
		String expectedOutput = expected.getOutput();
		String expectedInput = expected.getInput();
		String actualOutput = actual.getOutput();
		String actualInput = actual.getInput();
		
		// Handle null cases
		if (expectedOutput == null) expectedOutput = "";
		if (expectedInput == null) expectedInput = "";
		if (actualOutput == null) actualOutput = "";
		if (actualInput == null) actualInput = "";
		
		// Parse comma-separated fields
		List<String> expectedOutputFields = parseFields(expectedOutput);
		List<String> expectedInputFields = parseFields(expectedInput);
		List<String> actualOutputFields = parseFields(actualOutput);
		List<String> actualInputFields = parseFields(actualInput);
		
		// Must have same number of fields
		if (expectedOutputFields.size() != actualOutputFields.size() ||
			expectedInputFields.size() != actualInputFields.size()) {
			return false;
		}
		
		// If different number of output vs input fields, can't match
		if (expectedOutputFields.size() != expectedInputFields.size()) {
			// But allow this case - just check each independently
			return listsMatchAsMultisets(expectedOutputFields, actualOutputFields) &&
				   listsMatchAsMultisets(expectedInputFields, actualInputFields);
		}
		
		// Try to find a permutation that works for both input and output
		return findMatchingPermutation(expectedOutputFields, expectedInputFields, 
									  actualOutputFields, actualInputFields);
	}

	/**
	 * Parse comma-separated fields, trimming whitespace.
	 */
	private static List<String> parseFields(String fieldString) {
		if (fieldString == null || fieldString.trim().isEmpty()) {
			return new ArrayList<>();
		}
		
		List<String> fields = new ArrayList<>();
		for (String field : fieldString.split(",")) {
			String trimmed = field.trim();
			if (!trimmed.isEmpty()) {
				fields.add(trimmed);
			}
		}
		return fields;
	}

	/**
	 * Check if two lists contain the same elements (as multisets).
	 */
	private static boolean listsMatchAsMultisets(List<String> list1, List<String> list2) {
		if (list1.size() != list2.size()) {
			return false;
		}
		
		Map<String, Integer> count1 = new HashMap<>();
		Map<String, Integer> count2 = new HashMap<>();
		
		for (String item : list1) {
			count1.put(item, count1.getOrDefault(item, 0) + 1);
		}
		for (String item : list2) {
			count2.put(item, count2.getOrDefault(item, 0) + 1);
		}
		
		return count1.equals(count2);
	}

	/**
	 * Find if there's a permutation such that both output and input lists match
	 * the same reordering pattern.
	 */
	private static boolean findMatchingPermutation(List<String> expectedOutput, List<String> expectedInput,
												  List<String> actualOutput, List<String> actualInput) {
		int n = expectedOutput.size();
		if (n == 0) {
			return true;
		}
		
		// Try all possible permutations of indices
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			indices.add(i);
		}
		
		return tryPermutation(indices, 0, expectedOutput, expectedInput, actualOutput, actualInput);
	}

	/**
	 * Recursively try all permutations to see if any matches both output and input.
	 */
	private static boolean tryPermutation(List<Integer> indices, int pos,
										 List<String> expectedOutput, List<String> expectedInput,
										 List<String> actualOutput, List<String> actualInput) {
		if (pos == indices.size()) {
			// Check if this permutation works
			for (int i = 0; i < indices.size(); i++) {
				int mappedIndex = indices.get(i);
				if (mappedIndex >= actualOutput.size() || mappedIndex >= actualInput.size()) {
					return false;
				}
				if (!expectedOutput.get(i).equals(actualOutput.get(mappedIndex)) ||
					!expectedInput.get(i).equals(actualInput.get(mappedIndex))) {
					return false;
				}
			}
			return true;
		}
		
		// Try swapping with each remaining position
		for (int i = pos; i < indices.size(); i++) {
			// Swap
			java.util.Collections.swap(indices, pos, i);
			if (tryPermutation(indices, pos + 1, expectedOutput, expectedInput, actualOutput, actualInput)) {
				return true;
			}
			// Swap back
			java.util.Collections.swap(indices, pos, i);
		}
		
		return false;
	}
	
	private static void testSplitEdgeCase() {
		try {
			// Create expected edge
			TaskDto.EdgeDto expectedEdge = new TaskDto.EdgeDto();
			expectedEdge.setFrom("instruction");
			expectedEdge.setTo("manage_skill");
			TaskDto.ConnectionDto expectedConnection = new TaskDto.ConnectionDto();
			expectedConnection.setOutput("action, skill_name, status");
			expectedConnection.setInput("action, skill_name, status");
			expectedEdge.setConnection(expectedConnection);
			
			// Create actual edges - Split edge case:
			// Rule: "from" can be different but "to" must be same as expected
			TaskDto.EdgeDto actualEdge1 = new TaskDto.EdgeDto();
			actualEdge1.setFrom("discover_user_employee_entities"); // Different from expected
			actualEdge1.setTo("manage_skill"); // Same as expected ✓
			TaskDto.ConnectionDto connection1 = new TaskDto.ConnectionDto();
			connection1.setOutput("results[0].status");
			connection1.setInput("status");
			actualEdge1.setConnection(connection1);
			
			TaskDto.EdgeDto actualEdge2 = new TaskDto.EdgeDto();
			actualEdge2.setFrom("instruction"); // Same as expected
			actualEdge2.setTo("manage_skill"); // Same as expected ✓
			TaskDto.ConnectionDto connection2 = new TaskDto.ConnectionDto();
			connection2.setOutput("action, skill_name");
			connection2.setInput("action, skill_name");
			actualEdge2.setConnection(connection2);
			
			// Debug field parsing
			System.out.println("Expected input: " + expectedEdge.getConnection().getInput());
			List<String> expectedFields = parseFields(expectedEdge.getConnection().getInput());
			System.out.println("Expected parsed fields: " + expectedFields);
			
			System.out.println("Actual edge 1 input: " + actualEdge1.getConnection().getInput());
			List<String> actualFields1 = parseFields(actualEdge1.getConnection().getInput());
			System.out.println("Actual 1 parsed fields: " + actualFields1);
			
			System.out.println("Actual edge 2 input: " + actualEdge2.getConnection().getInput());
			List<String> actualFields2 = parseFields(actualEdge2.getConnection().getInput());
			System.out.println("Actual 2 parsed fields: " + actualFields2);
			
			// Test the matching
			List<TaskDto.EdgeDto> actualCopy = new ArrayList<>();
			actualCopy.add(actualEdge1);
			actualCopy.add(actualEdge2);
			
			System.out.println("Testing split edge matching...");
			System.out.println("Original actual copy size: " + actualCopy.size());
			
			boolean result = tryMatchSplitEdge(expectedEdge, actualCopy);
			
			System.out.println("Split edge matching test result: " + result);
			System.out.println("Remaining actual edges after matching: " + actualCopy.size());
			if (result) {
				System.out.println("SUCCESS: Split edge was properly matched!");
				if (actualCopy.size() == 0) {
					System.out.println("All actual edges were consumed, which is expected");
				} else {
					System.out.println("WARNING: Some actual edges remain: " + actualCopy.size());
				}
			} else {
				System.out.println("FAILED: Split edge was not matched");
				System.out.println("This indicates the algorithm needs further improvement");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void testManyToManyEdgeCase() {
		try {
			System.out.println("\n=== Testing Many-to-Many Edge Matching ===");
			
			// Create expected edges (2 separate edges)
			TaskDto.EdgeDto expectedEdge1 = new TaskDto.EdgeDto();
			expectedEdge1.setFrom("discover_department_entities");
			expectedEdge1.setTo("manage_job_position");
			TaskDto.ConnectionDto expectedConnection1 = new TaskDto.ConnectionDto();
			expectedConnection1.setOutput("results[0].department_id");
			expectedConnection1.setInput("department_id");
			expectedEdge1.setConnection(expectedConnection1);
			
			TaskDto.EdgeDto expectedEdge2 = new TaskDto.EdgeDto();
			expectedEdge2.setFrom("instruction");
			expectedEdge2.setTo("manage_job_position");
			TaskDto.ConnectionDto expectedConnection2 = new TaskDto.ConnectionDto();
			expectedConnection2.setOutput("action, employment_type, hourly_rate_max, hourly_rate_min, job_level, status, title");
			expectedConnection2.setInput("action, employment_type, hourly_rate_max, hourly_rate_min, job_level, status, title");
			expectedEdge2.setConnection(expectedConnection2);
			
			// Create actual edges (2 edges with different field distribution)
			TaskDto.EdgeDto actualEdge1 = new TaskDto.EdgeDto();
			actualEdge1.setFrom("discover_department_entities");
			actualEdge1.setTo("manage_job_position");
			TaskDto.ConnectionDto actualConnection1 = new TaskDto.ConnectionDto();
			actualConnection1.setOutput("results[0].department_id, results[0].status");
			actualConnection1.setInput("department_id, status");
			actualEdge1.setConnection(actualConnection1);
			
			TaskDto.EdgeDto actualEdge2 = new TaskDto.EdgeDto();
			actualEdge2.setFrom("instruction");
			actualEdge2.setTo("manage_job_position");
			TaskDto.ConnectionDto actualConnection2 = new TaskDto.ConnectionDto();
			actualConnection2.setOutput("action, employment_type, hourly_rate_max, hourly_rate_min, job_level, title");
			actualConnection2.setInput("action, employment_type, hourly_rate_max, hourly_rate_min, job_level, title");
			actualEdge2.setConnection(actualConnection2);
			
			// Test many-to-many matching
			List<TaskDto.EdgeDto> expected = new ArrayList<>();
			expected.add(expectedEdge1);
			expected.add(expectedEdge2);
			
			List<TaskDto.EdgeDto> actual = new ArrayList<>();
			actual.add(actualEdge1);
			actual.add(actualEdge2);
			
			boolean result = compareEdgesFlexibly(expected, actual);
			
			System.out.println("Many-to-many edge matching test result: " + result);
			if (result) {
				System.out.println("SUCCESS: Many-to-many edge matching works correctly!");
			} else {
				System.out.println("FAILED: Many-to-many edge matching needs improvement");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
