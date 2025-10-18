package com.amazon.agenticworkstation.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.amazon.agenticworkstation.action.ActionGeneratorConstants.*;

/**
 * Manages loading and caching of interface method mappings
 */
public final class InterfaceMappingLoader {
	private static final Logger log = LoggerFactory.getLogger(InterfaceMappingLoader.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	// Cache for interface method mappings
	private static final Map<String, Map<String, Object>> INTERFACE_MAPPINGS_CACHE = new HashMap<>();

	private InterfaceMappingLoader() {
		// Private constructor to prevent instantiation
	}

	/**
	 * Load interface method mappings from the environment's
	 * interface_method_mappings.json file
	 * 
	 * @param envName Environment name
	 * @return Interface mappings map
	 * @throws IOException if mappings file not found or cannot be loaded
	 */
	public static Map<String, Object> loadInterfaceMappings(String envName) throws IOException {
		// Check cache first
		if (INTERFACE_MAPPINGS_CACHE.containsKey(envName)) {
			log.debug("Using cached interface mappings for environment: {}", envName);
			return INTERFACE_MAPPINGS_CACHE.get(envName);
		}

		String currentDir = System.getProperty(USER_DIR_PROPERTY);
		Path workspaceRoot = Paths.get(currentDir);

		// If we're in backend directory, go up one level
		if (workspaceRoot.endsWith(BACKEND_DIR)) {
			workspaceRoot = workspaceRoot.getParent();
		}

		// Try multiple possible locations for the mappings file
		Path mappingsPath = null;
		
		// Location 1: Inside agenticWorkstation/amazon-tau-bench-tasks-main
		Path path1 = workspaceRoot.resolve(TAU_BENCH_DIR).resolve(ENVS_DIR).resolve(envName)
				.resolve(INTERFACE_MAPPINGS_FILE);
		
		// Location 2: Sibling to agenticWorkstation (go up one more level)
		Path path2 = workspaceRoot.getParent().resolve(TAU_BENCH_DIR).resolve(ENVS_DIR).resolve(envName)
				.resolve(INTERFACE_MAPPINGS_FILE);
		
		if (Files.exists(path1)) {
			mappingsPath = path1;
			log.debug("Found interface mappings at location 1: {}", path1);
		} else if (Files.exists(path2)) {
			mappingsPath = path2;
			log.debug("Found interface mappings at location 2: {}", path2);
		} else {
			throw new IOException("Interface mappings file not found. Tried:\n" 
					+ "  1. " + path1 + "\n"
					+ "  2. " + path2 + "\n"
					+ "This file is required for environment '" + envName + "'.");
		}

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> mappings = objectMapper.readValue(mappingsPath.toFile(), Map.class);
			INTERFACE_MAPPINGS_CACHE.put(envName, mappings);
			log.info("Loaded interface mappings for environment '{}' from: {}", envName, mappingsPath);
			return mappings;
		} catch (Exception e) {
			throw new IOException("Failed to load interface mappings from " + mappingsPath + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Extract the auto-audit setting from interface mappings
	 * 
	 * @param interfaceMappings Interface mappings loaded from JSON
	 * @param envName           Environment name for logging
	 * @return true if auto-audit is enabled, false otherwise
	 */
	public static boolean extractAutoAuditSetting(Map<String, Object> interfaceMappings, String envName) {
		if (interfaceMappings == null || interfaceMappings.isEmpty()) {
			log.warn("Interface mappings are null or empty for environment '{}', auto-audit disabled", envName);
			return false;
		}

		Object isAuditLog = interfaceMappings.get(IS_AUDIT_LOG_KEY);
		if (isAuditLog == null) {
			log.warn("'{}' field not found in interface mappings for environment '{}', auto-audit disabled",
					IS_AUDIT_LOG_KEY, envName);
			return false;
		}

		if (isAuditLog instanceof Boolean) {
			return (Boolean) isAuditLog;
		}

		log.warn("'{}' field is not a boolean in environment '{}' (type: {}), auto-audit disabled", 
				IS_AUDIT_LOG_KEY, envName, isAuditLog.getClass().getSimpleName());
		return false;
	}

	/**
	 * Get the audit log function name for the specified interface
	 * 
	 * @param interfaceName     Interface name (e.g., "interface_1")
	 * @param interfaceMappings Interface method mappings (required)
	 * @return Audit log function name
	 * @throws IOException if mappings are not available or audit function not found
	 */
	public static String getAuditLogFunction(String interfaceName, Map<String, Object> interfaceMappings)
			throws IOException {
		if (interfaceMappings == null || interfaceMappings.isEmpty()) {
			throw new IOException("Interface mappings are required but not available for interface: " + interfaceName);
		}

		Object methodMappings = interfaceMappings.get(METHOD_MAPPINGS_KEY);
		if (!(methodMappings instanceof Map)) {
			throw new IOException(
					"Invalid interface mappings structure: missing '" + METHOD_MAPPINGS_KEY + "' for " + interfaceName);
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> mappings = (Map<String, Object>) methodMappings;
		Object auditMapping = mappings.get(MANAGE_AUDIT_LOGS_KEY);

		if (!(auditMapping instanceof Map)) {
			throw new IOException("Audit log mapping not found in interface mappings for " + interfaceName);
		}

		@SuppressWarnings("unchecked")
		Map<String, String> auditMethods = (Map<String, String>) auditMapping;
		String functionName = auditMethods.get(interfaceName);

		if (functionName == null) {
			throw new IOException("Audit log function not found for " + interfaceName + " in interface mappings");
		}

		log.debug("Found audit log function '{}' from mappings for {}", functionName, interfaceName);
		return functionName;
	}

	/**
	 * Check if an action is an audit log function
	 * 
	 * @param actionName        Action function name
	 * @param interfaceMappings Interface method mappings (can be null)
	 * @return true if this is an audit log function
	 */
	public static boolean isAuditLogFunction(String actionName, Map<String, Object> interfaceMappings) {
		// Check if mappings indicate this is an audit log function
		if (interfaceMappings != null) {
			Object methodMappings = interfaceMappings.get(METHOD_MAPPINGS_KEY);
			if (methodMappings instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> mappings = (Map<String, Object>) methodMappings;
				Object auditMapping = mappings.get(MANAGE_AUDIT_LOGS_KEY);
				if (auditMapping instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, String> auditMethods = (Map<String, String>) auditMapping;
					return auditMethods.values().stream().anyMatch(method -> method.equals(actionName));
				}
			}
		}

		return false;
	}

	/**
	 * Check if an action is a CRUD operation based on its category's is_crud property
	 * 
	 * @param actionName        Action function name
	 * @param interfaceMappings Interface method mappings (required)
	 * @return true if this action belongs to a category with is_crud=true
	 */
	public static boolean isCrudOperation(String actionName, Map<String, Object> interfaceMappings) {
		if (interfaceMappings == null || interfaceMappings.isEmpty()) {
			log.debug("Interface mappings not available for action '{}', cannot determine CRUD status", actionName);
			return false;
		}

		// Get method_mappings
		Object methodMappingsObj = interfaceMappings.get(METHOD_MAPPINGS_KEY);
		if (!(methodMappingsObj instanceof Map)) {
			log.debug("method_mappings not found in interface mappings for action '{}'", actionName);
			return false;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> methodMappings = (Map<String, Object>) methodMappingsObj;

		// Find the method entry that contains this action name
		String categoryName = null;
		for (Map.Entry<String, Object> entry : methodMappings.entrySet()) {
			if (entry.getValue() instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> methodInfo = (Map<String, Object>) entry.getValue();
				
				// Check if this action name matches any interface variant
				for (Object value : methodInfo.values()) {
					if (actionName.equals(value)) {
						categoryName = (String) methodInfo.get("category");
						break;
					}
				}
				
				if (categoryName != null) {
					break;
				}
			}
		}

		if (categoryName == null) {
			log.debug("Action '{}' not found in method_mappings", actionName);
			return false;
		}

		// Get method_categories
		Object methodCategoriesObj = interfaceMappings.get("method_categories");
		if (!(methodCategoriesObj instanceof Map)) {
			log.debug("method_categories not found in interface mappings");
			return false;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> methodCategories = (Map<String, Object>) methodCategoriesObj;

		// Get the category info
		Object categoryInfoObj = methodCategories.get(categoryName);
		if (!(categoryInfoObj instanceof Map)) {
			log.debug("Category '{}' not found in method_categories for action '{}'", categoryName, actionName);
			return false;
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> categoryInfo = (Map<String, Object>) categoryInfoObj;

		// Get is_crud property
		Object isCrudObj = categoryInfo.get("is_crud");
		if (isCrudObj instanceof Boolean) {
			boolean isCrud = (Boolean) isCrudObj;
			log.debug("Action '{}' belongs to category '{}' with is_crud={}", actionName, categoryName, isCrud);
			return isCrud;
		}

		log.debug("is_crud property not found or not a boolean for category '{}' of action '{}'", 
				categoryName, actionName);
		return false;
	}
}
