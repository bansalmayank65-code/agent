package com.amazon.agenticworkstation.action;

import java.util.Arrays;
import java.util.List;

/**
 * Constants used across the Action Generator components
 */
public final class ActionGeneratorConstants {
	
	// Path constants for environment data
	public static final String TAU_BENCH_DIR = "amazon-tau-bench-tasks-main";
	public static final String ENVS_DIR = "envs";
	public static final String DATA_DIR = "data";
	public static final String INTERFACE_MAPPINGS_FILE = "interface_method_mappings.json";
	
	// Directory structure
	public static final String BACKEND_DIR = "backend";
	public static final String USER_DIR_PROPERTY = "user.dir";
	
	// File extensions
	public static final String JSON_EXTENSION = ".json";
	
	// Temp file naming
	public static final String TEMP_FILE_PREFIX = "env_data_";
	public static final String TEMP_FILE_SUFFIX = ".json";
	
	// CRUD operation patterns to detect
	public static final List<String> CRUD_OPERATIONS = Arrays.asList("create", "update", "delete", "remove");
	
	// Audit log configuration keys
	public static final String IS_AUDIT_LOG_KEY = "is_audit_log";
	public static final String METHOD_MAPPINGS_KEY = "method_mappings";
	public static final String MANAGE_AUDIT_LOGS_KEY = "manage_audit_logs";
	
	// Parameter field names
	public static final String ACTION_FIELD = "action";
	public static final String OPERATION_FIELD = "operation";
	public static final String USER_ID_FIELD = "user_id";
	public static final String REFERENCE_TYPE_FIELD = "reference_type";
	public static final String REFERENCE_ID_FIELD = "reference_id";
	public static final String FIELD_NAME_FIELD = "field_name";
	public static final String OLD_VALUE_FIELD = "old_value";
	public static final String NEW_VALUE_FIELD = "new_value";
	
	// Common ID field names for extraction
	public static final String[] COMMON_ID_FIELDS = {
		"id", 
		"employee_id", 
		"department_id", 
		"user_id", 
		"log_id", 
		"position_id",
		"application_id", 
		"review_id"
	};
	
	// Parameter name variations
	public static final String ID_SUFFIX = "_id";
	public static final String CAMEL_CASE_ID_SUFFIX = "Id";
	public static final String UNDERSCORE = "_";
	public static final String EMPTY_STRING = "";
	
	// Parameter type names
	public static final String TYPE_STRING = "string";
	public static final String TYPE_NUMBER = "number";
	public static final String TYPE_INTEGER = "integer";
	public static final String TYPE_BOOLEAN = "boolean";
	public static final String TYPE_OBJECT = "object";
	public static final String TYPE_ARRAY = "array";
	
	// Interface naming
	public static final String INTERFACE_PREFIX = "interface_";
	
	// Audit log operation values
	public static final String OPERATION_CREATE = "create";
	public static final String OPERATION_UPDATE = "update";
	
	// Pluralization suffixes
	public static final String PLURAL_S = "s";
	public static final String PLURAL_ES = "es";
	public static final String PLURAL_IES = "ies";
	public static final String SINGULAR_Y = "y";
	public static final String SINGULAR_X = "x";
	public static final String SINGULAR_CH = "ch";
	
	// Function name patterns
	public static final String MANAGE_FUNCTION_PATTERN = "manage";
	
	private ActionGeneratorConstants() {
		// Private constructor to prevent instantiation
	}
}
