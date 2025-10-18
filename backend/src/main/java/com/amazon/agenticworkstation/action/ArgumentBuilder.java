package com.amazon.agenticworkstation.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.action.scenario.models.PythonFunctionMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.*;

/**
 * Builds arguments for actions by populating mandatory and optional fields
 */
public final class ArgumentBuilder {
	private static final Logger log = LoggerFactory.getLogger(ArgumentBuilder.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private ArgumentBuilder() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Build arguments for an action by populating mandatory and optional fields
	 */
	public static Map<String, Object> buildArguments(PythonFunctionMetadata metadata, Map<String, String> allInputs,
			Map<String, Object> previousOutputs, String actionName) throws IOException {

		Map<String, Object> arguments = new HashMap<>();
		List<String> missingRequired = new ArrayList<>();

		Map<String, PythonFunctionMetadata.ParameterInfo> parameters = metadata.getParameters();
		List<String> requiredParams = metadata.getRequiredParams();

		log.debug("Building arguments for action '{}'. Required: {}, Total params: {}", actionName,
				requiredParams.size(), parameters.size());

		// Process all parameters
		for (Map.Entry<String, PythonFunctionMetadata.ParameterInfo> entry : parameters.entrySet()) {
			String paramName = entry.getKey();
			PythonFunctionMetadata.ParameterInfo paramInfo = entry.getValue();
			boolean isRequired = requiredParams.contains(paramName);

			Object value = findParameterValue(paramName, paramInfo, allInputs, previousOutputs);

			if (value != null) {
				// Convert value to appropriate type
				Object typedValue = convertToType(value, paramInfo.getType(), paramName);
				arguments.put(paramName, typedValue);
				log.debug("Populated parameter '{}' = {} (required: {})", paramName, typedValue, isRequired);
			} else if (isRequired) {
				missingRequired.add(paramName);
				log.warn("Missing required parameter: {}", paramName);
			}
		}

		// Check if all required parameters are satisfied
		if (!missingRequired.isEmpty()) {
			String errorMsg = String.format(
					"Action '%s' is missing required parameters: %s. " + "Available inputs: %s. Description: %s",
					actionName, missingRequired, allInputs.keySet(), metadata.getDescription());
			log.error(errorMsg);
			throw new IOException(errorMsg);
		}

		log.debug("Successfully built {} arguments for action '{}'", arguments.size(), actionName);
		return arguments;
	}

	/**
	 * Find value for a parameter from allInputs or previousOutputs
	 */
	private static Object findParameterValue(String paramName, PythonFunctionMetadata.ParameterInfo paramInfo,
			Map<String, String> allInputs, Map<String, Object> previousOutputs) {

		// First, try exact match in allInputs
		if (allInputs.containsKey(paramName)) {
			return allInputs.get(paramName);
		}

		// Try to find in previous outputs
		if (previousOutputs.containsKey(paramName)) {
			return previousOutputs.get(paramName);
		}

		// Try common variations and mappings
		String value = findWithVariations(paramName, allInputs, previousOutputs);
		if (value != null) {
			return value;
		}

		return null;
	}

	/**
	 * Find value with common name variations
	 */
	private static String findWithVariations(String paramName, Map<String, String> allInputs,
			Map<String, Object> previousOutputs) {

		// Try camelCase version
		String camelCase = toCamelCase(paramName);
		if (allInputs.containsKey(camelCase)) {
			return allInputs.get(camelCase);
		}

		// Try without underscores
		String noUnderscore = paramName.replace(UNDERSCORE, EMPTY_STRING);
		if (allInputs.containsKey(noUnderscore)) {
			return allInputs.get(noUnderscore);
		}

		// Try partial matches for common patterns
		if (paramName.endsWith(ID_SUFFIX)) {
			String baseName = paramName.substring(0, paramName.length() - 3);
			if (allInputs.containsKey(baseName)) {
				return allInputs.get(baseName);
			}
			if (allInputs.containsKey(baseName + CAMEL_CASE_ID_SUFFIX)) {
				return allInputs.get(baseName + CAMEL_CASE_ID_SUFFIX);
			}
		}

		// Check previous outputs for common result fields
		for (String idField : COMMON_ID_FIELDS) {
			if (paramName.equals(idField) && previousOutputs.containsKey(idField)) {
				Object val = previousOutputs.get(idField);
				return val != null ? val.toString() : null;
			}
		}

		return null;
	}

	/**
	 * Convert value to the specified type
	 */
	private static Object convertToType(Object value, String type, String paramName) {
		if (value == null) {
			return null;
		}

		String strValue = value.toString();

		try {
			switch (type) {
			case TYPE_STRING:
				return strValue;

			case TYPE_NUMBER:
			case TYPE_INTEGER:
				if (strValue.contains(".")) {
					return Double.parseDouble(strValue);
				} else {
					return Integer.parseInt(strValue);
				}

			case TYPE_BOOLEAN:
				return Boolean.parseBoolean(strValue);

			case TYPE_OBJECT:
			case TYPE_ARRAY:
				// Try to parse as JSON
				try {
					return objectMapper.readValue(strValue, Object.class);
				} catch (Exception e) {
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
	 * Convert snake_case to camelCase
	 */
	private static String toCamelCase(String snakeCase) {
		StringBuilder result = new StringBuilder();
		boolean capitalizeNext = false;

		for (char c : snakeCase.toCharArray()) {
			if (c == '_') {
				capitalizeNext = true;
			} else {
				if (capitalizeNext) {
					result.append(Character.toUpperCase(c));
					capitalizeNext = false;
				} else {
					result.append(c);
				}
			}
		}

		return result.toString();
	}

	/**
	 * Extract a value from multiple sources
	 */
	public static String extractValue(Map<String, Object> arguments, Map<String, String> allInputs,
			Map<String, Object> previousOutputs, String... keys) {
		for (String key : keys) {
			// Try arguments first
			if (arguments.containsKey(key)) {
				Object val = arguments.get(key);
				if (val != null)
					return val.toString();
			}
			// Try allInputs
			if (allInputs.containsKey(key)) {
				return allInputs.get(key);
			}
			// Try previousOutputs
			if (previousOutputs.containsKey(key)) {
				Object val = previousOutputs.get(key);
				if (val != null)
					return val.toString();
			}
		}
		return null;
	}
}
