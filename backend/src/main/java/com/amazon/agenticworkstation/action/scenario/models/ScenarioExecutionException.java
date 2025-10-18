package com.amazon.agenticworkstation.action.scenario.models;

/**
 * Exception thrown when scenario execution fails. Provides detailed information
 * about the failure point and context.
 */
public class ScenarioExecutionException extends Exception {
	private static final long serialVersionUID = 5889788683678904863L;
	private final String scenarioName;
	private final String stepId;
	private final String actionName;
	private final String failureReason;

	public ScenarioExecutionException(String scenarioName, String stepId, String actionName, String failureReason) {
		super(buildMessage(scenarioName, stepId, actionName, failureReason));
		this.scenarioName = scenarioName;
		this.stepId = stepId;
		this.actionName = actionName;
		this.failureReason = failureReason;
	}

	public ScenarioExecutionException(String scenarioName, String stepId, String actionName, String failureReason,
			Throwable cause) {
		super(buildMessage(scenarioName, stepId, actionName, failureReason), cause);
		this.scenarioName = scenarioName;
		this.stepId = stepId;
		this.actionName = actionName;
		this.failureReason = failureReason;
	}

	public ScenarioExecutionException(String scenarioName, String failureReason) {
		super(buildMessage(scenarioName, failureReason));
		this.scenarioName = scenarioName;
		this.stepId = null;
		this.actionName = null;
		this.failureReason = failureReason;
	}

	public ScenarioExecutionException(String scenarioName, String failureReason, Throwable cause) {
		super(buildMessage(scenarioName, failureReason), cause);
		this.scenarioName = scenarioName;
		this.stepId = null;
		this.actionName = null;
		this.failureReason = failureReason;
	}

	private static String buildMessage(String scenarioName, String stepId, String actionName, String failureReason) {
		return String.format(
				"Scenario execution failed%n" + "Scenario: %s%n" + "Step ID: %s%n" + "Action: %s%n" + "Reason: %s",
				scenarioName, stepId, actionName, failureReason);
	}

	private static String buildMessage(String scenarioName, String failureReason) {
		return String.format("Scenario execution failed%n" + "Scenario: %s%n" + "Reason: %s", scenarioName,
				failureReason);
	}

	public String getScenarioName() {
		return scenarioName;
	}

	public String getStepId() {
		return stepId;
	}

	public String getActionName() {
		return actionName;
	}

	public String getFailureReason() {
		return failureReason;
	}
}
