package com.amazon.agenticworkstation.action.scenario.models;

import java.util.List;

/**
 * Model class to hold scenario metadata for listing and discovery.
 * 
 * Contains all the information needed to display a scenario to the user,
 * including its name, description, environment, interface, and required input
 * fields.
 */
public class ScenarioMetadata {

	private String scenarioName;
	private String description;
	private String environment;
	private int interfaceNumber;
	private List<ScenarioInputDefinition> requiredInputs;
	private String className;

	public ScenarioMetadata() {
	}

	public ScenarioMetadata(String scenarioName, String description, String environment, int interfaceNumber,
			List<ScenarioInputDefinition> requiredInputs, String className) {
		this.scenarioName = scenarioName;
		this.description = description;
		this.environment = environment;
		this.interfaceNumber = interfaceNumber;
		this.requiredInputs = requiredInputs;
		this.className = className;
	}

	// Getters and Setters

	public String getScenarioName() {
		return scenarioName;
	}

	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public int getInterfaceNumber() {
		return interfaceNumber;
	}

	public void setInterfaceNumber(int interfaceNumber) {
		this.interfaceNumber = interfaceNumber;
	}

	public List<ScenarioInputDefinition> getRequiredInputs() {
		return requiredInputs;
	}

	public void setRequiredInputs(List<ScenarioInputDefinition> requiredInputs) {
		this.requiredInputs = requiredInputs;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	public String toString() {
		return "ScenarioMetadata{" + "scenarioName='" + scenarioName + '\'' + ", description='" + description + '\''
				+ ", environment='" + environment + '\'' + ", interfaceNumber=" + interfaceNumber + ", className='"
				+ className + '\'' + ", requiredInputs=" + requiredInputs + '}';
	}
}
