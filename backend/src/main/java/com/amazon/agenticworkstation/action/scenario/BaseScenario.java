package com.amazon.agenticworkstation.action.scenario;

import java.util.List;
import java.util.Map;

import com.amazon.agenticworkstation.action.scenario.models.ScenarioConfig;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioInputDefinition;

/**
 * Base interface for all scenario definitions.
 * 
 * Each scenario implementation must provide: 1. The scenario configuration
 * (steps, actions, mappings) 2. The list of required inputs for the UI 3.
 * Metadata about the scenario
 * 
 * This allows the UI to: - Dynamically generate input forms - Display scenario
 * information - Validate inputs before execution
 */
public interface BaseScenario {

	/**
	 * Validate that a required parameter exists and is not null/empty.
	 * 
	 * @param parameters The parameters map
	 * @param paramName  The parameter name to validate
	 * @param paramType  The parameter type (for better error messages)
	 * @throws IllegalArgumentException if parameter is missing or invalid
	 */
	public default void validateRequiredParameter(Map<String, Object> parameters, String paramName, String paramType) {
		Object value = parameters.get(paramName);
		if (value == null) {
			throw new IllegalArgumentException(paramName + " is required (type: " + paramType + ")");
		}
		if (value instanceof String && ((String) value).trim().isEmpty()) {
			throw new IllegalArgumentException(paramName + " cannot be empty (type: " + paramType + ")");
		}
	}

	/**
	 * Get the scenario configuration.
	 * 
	 * @return ScenarioConfig containing all steps and mappings
	 */
	ScenarioConfig getScenarioConfig();

	/**
	 * Get the list of required inputs for this scenario. This information is used
	 * by the UI to generate input forms.
	 * 
	 * @return List of input definitions
	 */
	List<ScenarioInputDefinition> getRequiredInputs();

	/**
	 * Get the scenario name (unique identifier).
	 * 
	 * @return Scenario name
	 */
	default String getScenarioName() {
		return getScenarioConfig().getScenarioName();
	}

	/**
	 * Get the scenario description.
	 * 
	 * @return Human-readable description
	 */
	default String getDescription() {
		return getScenarioConfig().getDescription();
	}

	/**
	 * Get the environment name this scenario operates in.
	 * 
	 * @return Environment name
	 */
	default String getEnvironment() {
		return getScenarioConfig().getEnvName();
	}

	/**
	 * Get the interface number this scenario uses.
	 * 
	 * @return Interface number
	 */
	default int getInterfaceNumber() {
		return getScenarioConfig().getInterfaceNumber();
	}

	/**
	 * Get all scenario inputs with generated filters.
	 * 
	 * This method prepares the complete scenario inputs map including filter
	 * patterns needed by the scenario steps.
	 * 
	 * @return Complete scenario inputs with generated filters
	 */
	Map<String, Object> getScenarioInputs();
}
