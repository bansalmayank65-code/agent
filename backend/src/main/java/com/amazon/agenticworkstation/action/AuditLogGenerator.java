package com.amazon.agenticworkstation.action;

import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.ACTION_FIELD;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.COMMON_ID_FIELDS;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.CRUD_OPERATIONS;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.FIELD_NAME_FIELD;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.MANAGE_FUNCTION_PATTERN;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.NEW_VALUE_FIELD;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.OLD_VALUE_FIELD;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.OPERATION_CREATE;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.OPERATION_FIELD;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.OPERATION_UPDATE;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.PLURAL_ES;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.PLURAL_IES;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.PLURAL_S;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.REFERENCE_ID_FIELD;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.REFERENCE_TYPE_FIELD;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.SINGULAR_CH;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.SINGULAR_X;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.SINGULAR_Y;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.UNDERSCORE;
import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.USER_ID_FIELD;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.dto.TaskDto;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles generation of audit log actions for CRUD operations
 */
public final class AuditLogGenerator {
	private static final Logger log = LoggerFactory.getLogger(AuditLogGenerator.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	private AuditLogGenerator() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Check if an action is a CRUD operation that requires audit logging
	 * 
	 * @param actionName        Action function name
	 * @param arguments         Action arguments
	 * @param interfaceMappings Interface method mappings
	 * @return true if this is a CRUD operation requiring audit
	 */
	public static boolean isCrudOperation(String actionName, Map<String, Object> arguments,
			Map<String, Object> interfaceMappings) {
		// Skip if it's already an audit log function
		if (InterfaceMappingLoader.isAuditLogFunction(actionName, interfaceMappings)) {
			return false;
		}

		// Primary method: Check using is_crud property from interface mappings
		if (interfaceMappings != null && !interfaceMappings.isEmpty()) {
			boolean isCrud = InterfaceMappingLoader.isCrudOperation(actionName, interfaceMappings);
			if (isCrud) {
				log.debug("Action '{}' identified as CRUD operation from interface mappings", actionName);
				return true;
			}
		}

		// Fallback method: Check if action name contains CRUD keywords
		String lowerActionName = actionName.toLowerCase();
		for (String operation : CRUD_OPERATIONS) {
			if (lowerActionName.contains(operation)) {
				log.debug("Detected CRUD operation '{}' in action '{}' (fallback method)", operation, actionName);
				return true;
			}
		}

		// Additional fallback: Check if arguments contain an "action" or "operation"
		// field with CRUD values
		Object actionArg = arguments.get(ACTION_FIELD);
		Object operationArg = arguments.get(OPERATION_FIELD);

		String actionValue = actionArg != null ? actionArg.toString().toLowerCase() : null;
		String operationValue = operationArg != null ? operationArg.toString().toLowerCase() : null;

		if (actionValue != null && CRUD_OPERATIONS.contains(actionValue)) {
			log.debug("Detected CRUD action argument '{}' in action '{}' (fallback method)", actionValue, actionName);
			return true;
		}

		if (operationValue != null && CRUD_OPERATIONS.contains(operationValue)) {
			log.debug("Detected CRUD operation argument '{}' in action '{}' (fallback method)", operationValue,
					actionName);
			return true;
		}

		return false;
	}

	/**
	 * Generate an audit log action for a CRUD operation
	 */
	public static TaskDto.ActionDto generateAuditLogAction(String actionName, Map<String, Object> arguments,
			Object output, String dataFilePath, String envName, int interfaceNumber,
			Map<String, Object> interfaceMappings, String performingUserId) {
		String interfaceName = "interface_" + interfaceNumber;
		try {
			// Get the appropriate audit log function for this interface
			String auditLogFunction = InterfaceMappingLoader.getAuditLogFunction(interfaceName, interfaceMappings);

			log.debug("Generating audit log using function '{}' for action '{}'", auditLogFunction, actionName);

			// Extract audit log parameters
			Map<String, Object> auditArgs = buildAuditLogArguments(actionName, arguments, output, auditLogFunction,
					performingUserId);

			if (auditArgs == null || auditArgs.isEmpty()) {
				log.error("Could not build audit log arguments for action '{}'", actionName);
				throw new IllegalArgumentException("Audit log arguments are missing");
			}

			// Execute the audit log function
			String auditResultJson = PythonExecutorService.executePythonFunction(auditLogFunction, auditArgs,
					dataFilePath, envName, interfaceNumber);

			Object auditOutput = parseOutput(auditResultJson);

			// Create audit log ActionDto
			TaskDto.ActionDto auditAction = new TaskDto.ActionDto();
			auditAction.setName(auditLogFunction);
			auditAction.setArguments(auditArgs);
			auditAction.setOutput(auditOutput);

			log.info("Successfully generated audit log for action '{}'", actionName);
			return auditAction;

		} catch (Exception e) {
			log.error("Failed to generate audit log for action '{}': {}", actionName, e.getMessage(), e);
			throw new RuntimeException("Audit log generation failed for action '" + actionName + "'", e);
		}
	}

	/**
	 * Build arguments for audit log function based on the original action
	 * 
	 * @param actionName       Original action name
	 * @param arguments        Original action arguments
	 * @param output           Original action output
	 * @param allInputs        All inputs from instruction
	 * @param previousOutputs  Previous action outputs
	 * @param auditLogFunction Name of the audit log function
	 * @return Map of audit log arguments
	 */
	private static Map<String, Object> buildAuditLogArguments(String actionName, Map<String, Object> arguments,
			Object output, String auditLogFunction, String performingUserId) {

		Map<String, Object> auditArgs = new HashMap<>();

		// For manage_audit_logs, operation is always "create"
		if (auditLogFunction.contains(MANAGE_FUNCTION_PATTERN)) {
			auditArgs.put(OPERATION_FIELD, OPERATION_CREATE);
		}

		auditArgs.put(USER_ID_FIELD, performingUserId);

		// Extract action type from arguments or infer from action name
		String action = inferAuditAction(arguments);
		auditArgs.put(ACTION_FIELD, action);

		// Extract reference_type (e.g., "employees", "departments")
		String referenceType = inferReferenceType(actionName, arguments);
		if (referenceType == null) {
			log.warn("Cannot create audit log: reference_type could not be inferred");
			throw new RuntimeException("Audit log generation failed: reference_type missing");
		}
		auditArgs.put(REFERENCE_TYPE_FIELD, referenceType);

		// Extract reference_id from output or arguments
		String referenceId = extractReferenceId(output, arguments);
		if (referenceId == null) {
			log.warn("Cannot create audit log: reference_id not found");
			throw new RuntimeException("Audit log generation failed: reference_id missing");
		}
		auditArgs.put(REFERENCE_ID_FIELD, referenceId);

		// Optional fields
		extractOptionalAuditFields(auditArgs, arguments, output);

		log.debug("Built audit log arguments: {}", auditArgs);
		return auditArgs;
	}

	/**
	 * Infer the audit action type from arguments
	 */
	private static String inferAuditAction(Map<String, Object> arguments) {
		// Check for explicit action/operation argument
		Object actionArg = arguments.get(ACTION_FIELD);
		Object operationArg = arguments.get(OPERATION_FIELD);

		if (actionArg != null) {
			String action = actionArg.toString().toLowerCase();
			if (CRUD_OPERATIONS.contains(action)) {
				return action;
			}
		}

		if (operationArg != null) {
			String operation = operationArg.toString().toLowerCase();
			if (CRUD_OPERATIONS.contains(operation)) {
				return operation;
			}
		}

		// Default to "create"
		return OPERATION_CREATE;
	}

	/**
	 * Infer the reference type from action name
	 */
	private static String inferReferenceType(String actionName, Map<String, Object> arguments) {
		// Try to find reference_type in arguments first
		Object refType = arguments.get(REFERENCE_TYPE_FIELD);
		if (refType != null) {
			return refType.toString();
		}

		// Try to extract from action name (e.g., "manage_employee" -> "employees")
		String[] parts = actionName.split(UNDERSCORE);
		if (parts.length >= 2) {
			// Get the last part and pluralize it
			String entityName = parts[parts.length - 1];
			return pluralize(entityName);
		}

		// If we can't infer, throw a warning and return null - let caller handle it
		log.warn("Could not infer reference_type from action '{}' or arguments", actionName);
		return null;
	}

	/**
	 * Pluralize a singular entity name
	 */
	private static String pluralize(String entityName) {
		// Simple pluralization (add 's' or 'es')
		if (entityName.endsWith(PLURAL_S) || entityName.endsWith(SINGULAR_X) || entityName.endsWith(SINGULAR_CH)) {
			return entityName + PLURAL_ES;
		} else if (entityName.endsWith(SINGULAR_Y)) {
			return entityName.substring(0, entityName.length() - 1) + PLURAL_IES;
		} else {
			return entityName + PLURAL_S;
		}
	}

	/**
	 * Extract reference ID from output or arguments
	 */
	private static String extractReferenceId(Object output, Map<String, Object> arguments) {
		// Try to extract from output first
		if (output instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> outputMap = (Map<String, Object>) output;

			for (String field : COMMON_ID_FIELDS) {
				if (outputMap.containsKey(field)) {
					Object val = outputMap.get(field);
					if (val != null)
						return val.toString();
				}
			}
		}

		// Try to extract from arguments
		for (String field : COMMON_ID_FIELDS) {
			if (arguments.containsKey(field)) {
				Object val = arguments.get(field);
				if (val != null)
					return val.toString();
			}
		}

		return null;
	}

	/**
	 * Extract optional audit fields like field_name, old_value, new_value
	 */
	private static void extractOptionalAuditFields(Map<String, Object> auditArgs, Map<String, Object> arguments,
			Object output) {

		// For update operations, try to extract field information
		String action = auditArgs.get(ACTION_FIELD).toString();
		if (OPERATION_UPDATE.equals(action)) {
			// Check if arguments contain field_name, old_value, new_value
			if (arguments.containsKey(FIELD_NAME_FIELD)) {
				auditArgs.put(FIELD_NAME_FIELD, arguments.get(FIELD_NAME_FIELD));
			}
			if (arguments.containsKey(OLD_VALUE_FIELD)) {
				auditArgs.put(OLD_VALUE_FIELD, arguments.get(OLD_VALUE_FIELD));
			}
			if (arguments.containsKey(NEW_VALUE_FIELD)) {
				auditArgs.put(NEW_VALUE_FIELD, arguments.get(NEW_VALUE_FIELD));
			}
		}
	}

	/**
	 * Parse Python function output
	 */
	private static Object parseOutput(String resultJson) {
		try {
			// Try to parse as JSON
			return objectMapper.readValue(resultJson, Object.class);
		} catch (Exception e) {
			log.warn("Could not parse output as JSON, returning as string: {}", resultJson);
			return resultJson;
		}
	}
}
