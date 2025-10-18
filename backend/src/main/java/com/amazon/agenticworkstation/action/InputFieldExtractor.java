package com.amazon.agenticworkstation.action;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.action.scenario.models.PythonFunctionMetadata;

/**
 * Utility class to extract mandatory input fields from Python function
 * metadata. Returns a map of field names with empty/null values that can be
 * populated by the user.
 */
public final class InputFieldExtractor {
	private static final Logger log = LoggerFactory.getLogger(InputFieldExtractor.class);

	private InputFieldExtractor() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Extract all required input fields from multiple actions. Returns a nested map
	 * where each action name maps to its required fields.
	 * 
	 * @param actionNames     List of action function names
	 * @param envName         Environment name (e.g., "hr_experts")
	 * @param interfaceNumber Interface number (1-5)
	 * @return Map of action names to their required fields (LinkedHashMap to
	 *         preserve order)
	 * @throws IOException if metadata extraction fails
	 */
	public static Map<String, Map<String, String>> extractRequiredFields(List<String> actionNames, String envName,
			int interfaceNumber) throws IOException {

		log.info("Extracting required fields for {} actions in env: '{}' interface: {}", actionNames.size(), envName,
				interfaceNumber);

		// Use LinkedHashMap to preserve insertion order
		Map<String, Map<String, String>> actionFieldsMap = new LinkedHashMap<>();

		for (String actionName : actionNames) {
			log.debug("Extracting metadata for action: {}", actionName);

			// Extract metadata for this action
			PythonFunctionMetadata metadata = PythonExecutorService.extractMetadata(actionName, envName,
					interfaceNumber);

			// Get required parameters
			List<String> requiredParams = metadata.getRequiredParams();
			Map<String, PythonFunctionMetadata.ParameterInfo> parameters = metadata.getParameters();

			log.debug("Action '{}' has {} required parameters: {}", actionName, requiredParams.size(), requiredParams);

			// Create a map for this action's fields
			Map<String, String> actionFields = new LinkedHashMap<>();

			// Add all required parameters to the map with empty string values
			for (String paramName : requiredParams) {
				actionFields.put(paramName, "");
				log.debug("Added required field: {}", paramName);
			}

			// Also add fields whose description contains "required" keyword
			for (Map.Entry<String, PythonFunctionMetadata.ParameterInfo> paramEntry : parameters.entrySet()) {
				String paramName = paramEntry.getKey();
				PythonFunctionMetadata.ParameterInfo paramInfo = paramEntry.getValue();

				// Skip if already added as a required param
				if (actionFields.containsKey(paramName)) {
					continue;
				}

				// Check if description contains "required" (case-insensitive)
				String description = paramInfo.getDescription();
				if (description != null && description.toLowerCase().contains("required")) {
					actionFields.put(paramName, "");
					log.debug("Added field '{}' (description contains 'required'): {}", paramName, description);
				}
			}

			// Store this action's fields in the main map
			actionFieldsMap.put(actionName, actionFields);
			log.debug("Stored {} fields for action '{}'", actionFields.size(), actionName);
		}

		log.info("Extracted fields for {} actions", actionFieldsMap.size());
		return actionFieldsMap;
	}

	/**
	 * Extract all required input fields and return with null values instead of
	 * empty strings. Returns a nested map where each action name maps to its
	 * required fields with null values.
	 * 
	 * @param actionNames     List of action function names
	 * @param envName         Environment name (e.g., "hr_experts")
	 * @param interfaceNumber Interface number (1-5)
	 * @return Map of action names to their required fields with null values
	 * @throws IOException if metadata extraction fails
	 */
	public static Map<String, Map<String, String>> extractRequiredFieldsWithNull(List<String> actionNames,
			String envName, int interfaceNumber) throws IOException {

		Map<String, Map<String, String>> actionFieldsMap = extractRequiredFields(actionNames, envName, interfaceNumber);

		// Convert empty strings to null for each action
		Map<String, Map<String, String>> nullFieldsMap = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, String>> entry : actionFieldsMap.entrySet()) {
			String actionName = entry.getKey();
			Map<String, String> fields = entry.getValue();

			Map<String, String> nullFields = new LinkedHashMap<>();
			for (String key : fields.keySet()) {
				nullFields.put(key, null);
			}
			nullFieldsMap.put(actionName, nullFields);
		}

		return nullFieldsMap;
	}

	/**
	 * Extract only the strictly required parameters (excludes common fields).
	 * Returns a nested map where each action name maps to its strictly required
	 * fields.
	 * 
	 * @param actionNames     List of action function names
	 * @param envName         Environment name (e.g., "hr_experts")
	 * @param interfaceNumber Interface number (1-5)
	 * @return Map of action names to their strictly required fields
	 * @throws IOException if metadata extraction fails
	 */
	public static Map<String, Map<String, String>> extractStrictlyRequiredFields(List<String> actionNames,
			String envName, int interfaceNumber) throws IOException {

		log.info("Extracting strictly required fields for {} actions in env: '{}' interface: {}", actionNames.size(),
				envName, interfaceNumber);

		Map<String, Map<String, String>> actionFieldsMap = new LinkedHashMap<>();

		for (String actionName : actionNames) {
			log.debug("Extracting metadata for action: {}", actionName);

			PythonFunctionMetadata metadata = PythonExecutorService.extractMetadata(actionName, envName,
					interfaceNumber);

			List<String> requiredParams = metadata.getRequiredParams();

			// Create a map for this action's strictly required fields
			Map<String, String> actionFields = new LinkedHashMap<>();

			// Add only strictly required parameters
			for (String paramName : requiredParams) {
				actionFields.put(paramName, "");
				log.debug("Added strictly required field: {}", paramName);
			}

			// Store this action's fields in the main map
			actionFieldsMap.put(actionName, actionFields);
			log.debug("Stored {} strictly required fields for action '{}'", actionFields.size(), actionName);
		}

		log.info("Extracted strictly required fields for {} actions", actionFieldsMap.size());
		return actionFieldsMap;
	}

	/**
	 * Print the extracted fields in a format that can be copied to code.
	 * 
	 * @param actionFieldsMap Map of action names to their fields
	 */
	public static void printAsCode(Map<String, Map<String, String>> actionFieldsMap) {
		System.out.println("Map<String, Map<String, String>> actionFieldsMap = new LinkedHashMap<>();");
		System.out.println();

		for (Map.Entry<String, Map<String, String>> entry : actionFieldsMap.entrySet()) {
			String actionName = entry.getKey();
			Map<String, String> fields = entry.getValue();

			System.out.println("// Fields for action: " + actionName);
			System.out.println("Map<String, String> " + actionName + "Fields = new LinkedHashMap<>();");
			for (String fieldName : fields.keySet()) {
				System.out.println(actionName + "Fields.put(\"" + fieldName + "\", \"\");");
			}
			System.out.println("actionFieldsMap.put(\"" + actionName + "\", " + actionName + "Fields);");
			System.out.println();
		}
	}

	/**
	 * Print the extracted fields with example values (useful for documentation).
	 * 
	 * @param actionFieldsMap Map of action names to their fields
	 */
	public static void printWithExamples(Map<String, Map<String, String>> actionFieldsMap) {
		System.out.println("Map<String, Map<String, String>> actionFieldsMap = new LinkedHashMap<>();");
		System.out.println();

		for (Map.Entry<String, Map<String, String>> entry : actionFieldsMap.entrySet()) {
			String actionName = entry.getKey();
			Map<String, String> fields = entry.getValue();

			System.out.println("// Fields for action: " + actionName);
			System.out.println("Map<String, String> " + actionName + "Fields = new LinkedHashMap<>();");
			for (String fieldName : fields.keySet()) {
				String exampleValue = generateExampleValue(fieldName);
				System.out.println(actionName + "Fields.put(\"" + fieldName + "\", \"" + exampleValue + "\");");
			}
			System.out.println("actionFieldsMap.put(\"" + actionName + "\", " + actionName + "Fields);");
			System.out.println();
		}
	}

	/**
	 * Generate an example value for a field based on its name.
	 * 
	 * @param fieldName Field name
	 * @return Example value
	 */
	private static String generateExampleValue(String fieldName) {
		String lowerName = fieldName.toLowerCase();

		if (lowerName.equals("action") || lowerName.equals("operation")) {
			return "create";
		} else if (lowerName.contains("name")) {
			return "Example Name";
		} else if (lowerName.contains("_id") || lowerName.equals("id")) {
			return "1";
		} else if (lowerName.contains("date")) {
			return "2025-01-15";
		} else if (lowerName.contains("email")) {
			return "user@example.com";
		} else if (lowerName.contains("phone")) {
			return "+1234567890";
		} else {
			return "value";
		}
	}
}
