package com.amazon.agenticworkstation.constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constants used by EdgeGenerator for edge creation and field compatibility
 * matching.
 */
public final class EdgeGeneratorUtility {

	private EdgeGeneratorUtility() {
		// Private constructor to prevent instantiation
	}

	// ========== Edge Source Names ==========
	public static final String INSTRUCTION = "instruction";

	// ========== Field Names ==========
	public static final String ENTITY_TYPE = "entity_type";
	public static final String IDENTIFIER_TYPE = "identifier_type";
	public static final String OPERATION_TYPE = "operation_type";
	public static final String ACTION = "action";
	public static final String ACTION_TYPE = "action_type";
	public static final String OPERATION = "operation";
	public static final String REFERENCE_TYPE = "reference_type";
	public static final String TARGET_ENTITY_TYPE = "target_entity_type";
	public static final String FIELD_NAME = "field_name";
	public static final String REFERENCE_ID = "reference_id";
	public static final String TARGET_ENTITY_ID = "target_entity_id";
	public static final String OLD_VALUE = "old_value";
	public static final String EMAIL = "email";
	public static final String FIRST_NAME = "first_name";
	public static final String LAST_NAME = "last_name";
	public static final String SKILL_NAME = "skill_name";
	public static final String NAME = "name";
	public static final String STATUS = "status";
	public static final String COST_BASIS = "cost_basis";
	public static final String QUANTITY = "quantity";
	public static final String REQUESTER_EMAIL = "requester_email";
	public static final String REQUESTER_ID = "requester_id";
	public static final String USER_ID = "user_id";
	public static final String FUND_MANAGER_APPROVAL = "fund_manager_approval";
	public static final String APPROVAL = "approval";
	public static final String RESULTS = "results";

	// ========== Fields that MUST come from instruction ==========
	public static final List<String> INSTRUCTION_ONLY_FIELDS = List.of(ENTITY_TYPE, IDENTIFIER_TYPE, OPERATION_TYPE,
			ACTION, ACTION_TYPE, OPERATION, REFERENCE_TYPE, TARGET_ENTITY_TYPE, FIELD_NAME);

	// ========== Audit Action Names ==========
	public static final List<String> AUDIT_ACTION_NAMES = List.of("manage_audit_logs", "handle_audit_logs",
			"process_audit_logs", "administer_audit_logs", "execute_audit_logs", "generate_new_audit_trail",
			"create_new_audit_trail", "add_new_audit_trail", "register_new_audit_trail", "record_new_audit_trail",
			"open_audit_entry", "register_new_audit_trail");

	// ========== Audit-related field names ==========
	public static final List<String> AUDIT_VALID_FIELDS = List.of(REFERENCE_ID, TARGET_ENTITY_ID, OLD_VALUE);

	// ========== Field Priority for Sorting ==========
	public static final Map<String, Integer> FIELD_PRIORITY = createFieldPriorityMap();

	private static Map<String, Integer> createFieldPriorityMap() {
		Map<String, Integer> priority = new HashMap<>();
		priority.put(ENTITY_TYPE, 1);
		priority.put(IDENTIFIER_TYPE, 1);
		priority.put(OPERATION_TYPE, 1);
		priority.put(EMAIL, 2);
		priority.put(ACTION, 3);
		priority.put(ACTION_TYPE, 4);
		priority.put(FIRST_NAME, 4);
		priority.put(LAST_NAME, 5);
		priority.put(SKILL_NAME, 6); // HR task specific
		priority.put(NAME, 7);
		priority.put(STATUS, 8);
		priority.put(COST_BASIS, 9);
		priority.put(QUANTITY, 10);
		priority.put(OPERATION, 11); // HR task - should come after action, before reference_type
		priority.put(REFERENCE_TYPE, 12);
		priority.put(TARGET_ENTITY_TYPE, 12);
		return priority;
	}

	// ========== Default Field Priority ==========
	public static final int DEFAULT_FIELD_PRIORITY = 100;

	// ========== Field Compatibility Mappings ==========
	public static final Map<String, String> COMPATIBLE_FIELD_MAPPINGS = createCompatibleFieldMappings();

	private static Map<String, String> createCompatibleFieldMappings() {
		Map<String, String> mappings = new HashMap<>();

		// Email mappings
		mappings.put("results[0].contact_email", "notification_data.email");
		mappings.put("contact_email", "notification_data.email");

		// Subscription/Reference ID mappings
		mappings.put("results[0].subscription_id", "notification_data.reference_id");

		// Status mappings
		mappings.put("results[0].status", "filters.employment_status");

		// User ID mappings
		mappings.put("fund_data.manager_id", "results[0].user_id");
		mappings.put("manager_id", "results[0].user_id");
		mappings.put("employee_id", "results[0].user_id");
		mappings.put("requester_id", "results[0].user_id");
		mappings.put("reviewer_id", "results[0].user_id");
		mappings.put("approver_id", "results[0].user_id");
		mappings.put("approved_by", "results[0].user_id");
		mappings.put("approving_user_id", "results[0].user_id");
		mappings.put("uploaded_by", "results[0].user_id");
		mappings.put("created_by", "results[0].user_id");
		mappings.put("interviewer_id", "results[0].user_id");
		mappings.put("candidate_id", "results[0].user_id");
		mappings.put("recruiter_id", "results[0].user_id");

		// hr_talent
		mappings.put("related_entity_id", "candidate_id");

		// Employee ID specific mapping
		mappings.put("interviewer_id", "results[0].employee_id");

		// Approval mappings
		mappings.put("fund_data.fund_manager_approval", "approval_valid");
		mappings.put("fund_data.compliance_officer_approval", "approval_valid");
		mappings.put("fund_manager_approval", "approval_valid");
		mappings.put("compliance_officer_approval", "approval_valid");

		return mappings;
	}

	// ========== Special Field Name Mappings for Cleaning ==========
	public static final Map<String, String> FIELD_NAME_CLEANINGS = Map.of(REQUESTER_EMAIL, EMAIL, REQUESTER_ID, USER_ID,
			FUND_MANAGER_APPROVAL, APPROVAL);

	// ========== Delimiters ==========
	public static final String FIELD_DELIMITER = ", ";
	public static final String DOT_DELIMITER = ".";
	public static final String ARRAY_INDEX_PATTERN = "\\[\\d+\\]";

	// ========== Utility Methods ==========

	/**
	 * Extract field name from a key by removing array indices and getting the last
	 * part
	 */
	public static String getFieldName(String key) {
		if (key == null) {
			return "";
		}
		// Remove array indices and get last part
		String cleanKey = key.replaceAll(ARRAY_INDEX_PATTERN, "");
		String[] parts = cleanKey.split("\\" + DOT_DELIMITER);
		return parts[parts.length - 1];
	}

	/**
	 * Clean field name by removing filters prefix and mapping special cases Only
	 * cleans if isFromInstruction is true
	 */
	public static String cleanFieldName(String fieldName, boolean isFromInstruction) {
		if (fieldName == null) {
			return null;
		}

		// Only clean if this is from an instruction edge
		if (!isFromInstruction) {
			return fieldName; // Return as-is for action edges
		}

		// Generic cleaning: keep only the last part after any dot notation
		String cleaned = fieldName;
		int lastDotIndex = cleaned.lastIndexOf(DOT_DELIMITER);
		if (lastDotIndex != -1 && lastDotIndex < cleaned.length() - 1) {
			cleaned = cleaned.substring(lastDotIndex + 1);
		}

		// Map special cases for instruction output names
		return FIELD_NAME_CLEANINGS.getOrDefault(cleaned, cleaned);
	}

	/**
	 * Check if two field names are equivalent for comparison purposes
	 */
	public static boolean areFieldNamesEquivalent(String actionInput, String instructionInput) {
		if (actionInput == null || instructionInput == null) {
			return false;
		}

		// Clean the action input
		String cleanedActionInput = getFieldName(actionInput);

		// Instruction input is already cleaned, but we need to normalize it
		String cleanedInstructionInput = cleanFieldName(instructionInput, true);

		// Compare the cleaned field names
		return cleanedActionInput.equals(cleanedInstructionInput);
	}
}
