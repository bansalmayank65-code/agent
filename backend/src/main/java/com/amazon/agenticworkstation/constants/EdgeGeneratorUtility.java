package com.amazon.agenticworkstation.constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for EdgeGenerator operations. This class provides methods for
 * edge creation and field compatibility matching with environment-specific
 * behavior. Each instance can be configured with specific environment
 * parameters.
 */
public class EdgeGeneratorUtility {

	// Environment parameters for this instance
	private final String envName;
	private final Integer interfaceNum;

	private static final Logger logger = LoggerFactory.getLogger(EdgeGeneratorUtility.class);

	/**
	 * Constructor to create EdgeGeneratorUtility with environment parameters.
	 * 
	 * @param envName      Environment name for configuration (cannot be null or
	 *                     empty)
	 * @param interfaceNum Interface number for configuration (cannot be null)
	 */
	public EdgeGeneratorUtility(String envName, Integer interfaceNum) {
		if (envName == null || envName.trim().isEmpty()) {
			throw new IllegalArgumentException("Environment name (envName) is required and cannot be null or empty");
		}
		if (interfaceNum == null) {
			throw new IllegalArgumentException("Interface number (interfaceNum) is required and cannot be null");
		}
		this.envName = envName;
		this.interfaceNum = interfaceNum;
	}

	/**
	 * Get the environment name for this instance
	 * 
	 * @return Environment name
	 */
	public String getEnvName() {
		return envName;
	}

	/**
	 * Get the interface number for this instance
	 * 
	 * @return Interface number
	 */
	public Integer getInterfaceNum() {
		return interfaceNum;
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
	public static final String ACTOR_USER_ID = "actor_user_id";
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
			"open_audit_entry", "build_audit_entry", "make_audit_entry", "add_audit_entry", "create_audit_entry",
			"record_audit_log", "record_new_audit_trail", "generate_new_audit_trail");

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
	public Map<String, String> getCompatibleFieldMappings() {
		Map<String, String> mappings = new HashMap<>();

		if ("hr_experts".equalsIgnoreCase(envName)) {
			logger.info("Adding hr_experts field mappings");
			addHrExpertMapping(mappings);
		}

		if ("hr_talent_management".equalsIgnoreCase(envName)) {
			logger.info("Adding hr_talent_management field mappings");
			addHrTalentMappings(mappings);
		}

		if ("wiki_confluence".equalsIgnoreCase(envName)) {
			logger.info("Adding wiki_confluence field mappings");
			addWikiConfluenceMappings(mappings);
		}

		// Approval mappings
		mappings.put("fund_data.fund_manager_approval", "approval_valid");
		mappings.put("fund_data.compliance_officer_approval", "approval_valid");
		mappings.put("fund_manager_approval", "approval_valid");
		mappings.put("compliance_officer_approval", "approval_valid");
		mappings.put("hr_manager_approval", "approval_valid");
		mappings.put("dept_head_approval", "approval_valid");
		mappings.put("finance_manager_approval", "approval_valid");
		mappings.put("compliance_approval", "approval_valid");

		return mappings;
	}

	private static void addHrExpertMapping(Map<String, String> mappings) {
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
		mappings.put("interviewer_id", "results[0].employee_id");
	}

	private static void addHrTalentMappings(Map<String, String> mappings) {
		// Document entity relationships - where related_entity_id maps to specific
		// entity IDs
		mappings.put("candidate_id", "related_entity_id"); // when related_entity_type = "candidate"
		mappings.put("employee_id", "related_entity_id"); // when related_entity_type = "employee"
		mappings.put("offer_id", "related_entity_id"); // when related_entity_type = "offer"
		mappings.put("job_requisition_id", "related_entity_id"); // when related_entity_type = "job_requisition"
		mappings.put("onboarding_id", "related_entity_id"); // when related_entity_type = "onboarding"

		// Application workflow entity mappings
		mappings.put("posting_id", "job_postings.posting_id");
		mappings.put("resume_file_id", "documents.document_id");
		mappings.put("cover_letter_file_id", "documents.document_id");

		// Interview workflow entity mappings
		mappings.put("application_id", "applications.application_id");

		// Offer workflow entity mappings
		mappings.put("requisition_id", "job_requisitions.requisition_id");

		// Employee workflow entity mappings
		mappings.put("candidate_id", "candidates.candidate_id"); // in employees table

		// Payroll entity mappings
		mappings.put("payroll_input_id", "payroll_inputs.input_id");
		mappings.put("cycle_id", "payroll_cycles.cycle_id");
		mappings.put("payslip_id", "payslips.payslip_id");

		// Benefits entity mappings
		mappings.put("plan_id", "benefit_plans.plan_id");
		mappings.put("enrollment_id", "benefit_enrollments.enrollment_id");

		// IT Provisioning entity mappings
		mappings.put("task_id", "it_provisioning_tasks.task_id");

		// All field naming inconsistencies mapping to entities[0].user_id (from tool
		// responses)
		// OR direct user_id field reference in JSON data
		mappings.put("screened_by", "entities[0].user_id");
		mappings.put("shortlist_approved_by", "entities[0].user_id");
		mappings.put("completed_by", "entities[0].user_id");
		mappings.put("hiring_manager_id", "entities[0].user_id");
		mappings.put("hr_manager_approver", "entities[0].user_id");
		mappings.put("finance_manager_approver", "entities[0].user_id");
		mappings.put("dept_head_approver", "entities[0].user_id");
		mappings.put("compliance_approved_by", "entities[0].user_id");
		mappings.put("hr_manager_approved_by", "entities[0].user_id");
		mappings.put("reporting_manager_id", "entities[0].user_id");
		mappings.put("manager_id", "entities[0].user_id");
		mappings.put("uploaded_by", "entities[0].user_id");
		mappings.put("verified_by", "entities[0].user_id");
		mappings.put("assigned_by", "entities[0].user_id");
		mappings.put("assigned_to", "entities[0].user_id");
		mappings.put("created_by", "entities[0].user_id");
		mappings.put("user_id", "entities[0].user_id");
		mappings.put("recipient_user_id", "entities[0].user_id");
		mappings.put("manager_approved_by", "entities[0].user_id");
		mappings.put("approved_by", "entities[0].user_id");
	}

	private static void addWikiConfluenceMappings(Map<String, String> mappings) {
		// User ID field mappings - all map to user_data.user_id
		mappings.put("actor_user_id", "user_data.user_id");
		mappings.put("sender_user_id", "user_data.user_id");
		mappings.put("recipient_user_id", "user_data.user_id");
		mappings.put("requested_by_user_id", "user_data.user_id");
		mappings.put("created_by_user_id", "user_data.user_id");
		mappings.put("updated_by_user_id", "user_data.user_id");
		mappings.put("granted_by_user_id", "user_data.user_id");
		mappings.put("approver_user_id", "user_data.user_id");
		mappings.put("changed_by_user_id", "user_data.user_id");

		// Entity ID mappings - context-specific names for the same entity references
		mappings.put("target_entity_id", "entity_data.id");
		mappings.put("related_entity_id", "entity_data.id");
		mappings.put("space_data.space_id", "entity_id");

		// Grantee ID mappings - REVERSED to avoid override (multiple values for same
		// key)
		mappings.put("user_data.user_id", "grantee_id");
		mappings.put("group_data.group_id", "grantee_id");

		// Restricted entity ID mappings - REVERSED to avoid override (multiple values
		// for same key)
		mappings.put("user_data.user_id", "restricted_to_id");
		mappings.put("group_data.group_id", "restricted_to_id");

		// Watcher ID mappings - REVERSED to avoid override (multiple values for same
		// key)
		mappings.put("user_data.user_id", "watcher_id");
		mappings.put("group_data.group_id", "watcher_id");

		// Request/Step ID mappings - unique mapping, keep original direction
		mappings.put("step_id", "approval_data.request_id");

		// Page parent ID mappings - REVERSED to avoid override (multiple values for
		// same key)
		mappings.put("page_data.page_id", "parent_page_id");
		mappings.put("page_data.page_id", "new_parent_page_id");

		// Permission type mappings - unique mapping, keep original direction
		mappings.put("restriction_type", "permission_data.type");

		// Export job mappings - unique mapping, keep original direction
		mappings.put("export_id", "export_data.job_id");
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
	public String getFieldName(String key) {
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
	public String cleanFieldName(String fieldName, boolean isFromInstruction) {
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
	public boolean areFieldNamesEquivalent(String actionInput, String instructionInput) {
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
