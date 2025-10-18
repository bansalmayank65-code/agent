package com.amazon.agenticworkstation.action.scenario.models;

import java.util.Objects;

/**
 * Represents a mapping of input parameter for an action. Input can come from
 * scenario inputs, previous step outputs, or static values.
 */
public class InputMapping {
	private final String targetParameter;
	private final InputSource source;
	private final String sourceKey;
	private final Object staticValue;

	private InputMapping(Builder builder) {
		this.targetParameter = Objects.requireNonNull(builder.targetParameter, "targetParameter cannot be null");
		this.source = Objects.requireNonNull(builder.source, "source cannot be null");
		this.sourceKey = builder.sourceKey;
		this.staticValue = builder.staticValue;

		// Validation based on source type
		switch (source) {
		case SCENARIO_INPUT:
			if (sourceKey == null || sourceKey.isEmpty()) {
				throw new IllegalArgumentException("sourceKey is required for SCENARIO_INPUT source");
			}
			break;
		case PREVIOUS_STEP:
			if (sourceKey == null || sourceKey.isEmpty()) {
				throw new IllegalArgumentException(
						"sourceKey is required for PREVIOUS_STEP source (format: @actionName.fieldName, stepId.fieldName, or fieldName)");
			}
			break;
		case STATIC_VALUE:
			if (staticValue == null) {
				throw new IllegalArgumentException("staticValue is required for STATIC_VALUE source");
			}
			break;
		}
	}

	public String getTargetParameter() {
		return targetParameter;
	}

	public InputSource getSource() {
		return source;
	}

	public String getSourceKey() {
		return sourceKey;
	}

	public Object getStaticValue() {
		return staticValue;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Create a mapping from scenario input
	 */
	public static InputMapping fromScenarioInput(String targetParameter, String scenarioInputKey) {
		return builder().targetParameter(targetParameter).source(InputSource.SCENARIO_INPUT).sourceKey(scenarioInputKey)
				.build();
	}

	/**
	 * Create a mapping from previous step output by step ID Format:
	 * stepId.fieldName
	 *
	 * @param targetParameter The parameter name in the current action
	 * @param stepId          The id of the previous step to get output from
	 * @param outputField     The field name in the step's output
	 * @return InputMapping configured to retrieve value from specified step
	 */
	public static InputMapping fromPreviousStepOutput(String targetParameter, String stepId, String outputField) {
		return builder().targetParameter(targetParameter).source(InputSource.PREVIOUS_STEP)
				.sourceKey(stepId + "." + outputField).build();
	}

	/**
	 * Create a mapping from previous action output by action name
	 * Format: @actionName.fieldName
	 * 
	 * @param targetParameter The parameter name in the current action
	 * @param actionName      The name of the previous action to get output from
	 * @param outputField     The field name in the action's output
	 * @return InputMapping configured to retrieve value from specified action
	 */
	public static InputMapping fromPreviousActionOutput(String targetParameter, String actionName, String outputField) {
		return builder().targetParameter(targetParameter).source(InputSource.PREVIOUS_STEP)
				.sourceKey("@" + actionName + "." + outputField).build();
	}

	/**
	 * Create a mapping with static value
	 */
	public static InputMapping withStaticValue(String targetParameter, Object value) {
		return builder().targetParameter(targetParameter).source(InputSource.STATIC_VALUE).staticValue(value).build();
	}

	public static class Builder {
		private String targetParameter;
		private InputSource source;
		private String sourceKey;
		private Object staticValue;

		public Builder targetParameter(String targetParameter) {
			this.targetParameter = targetParameter;
			return this;
		}

		public Builder source(InputSource source) {
			this.source = source;
			return this;
		}

		public Builder sourceKey(String sourceKey) {
			this.sourceKey = sourceKey;
			return this;
		}

		public Builder staticValue(Object staticValue) {
			this.staticValue = staticValue;
			return this;
		}

		public InputMapping build() {
			return new InputMapping(this);
		}
	}

	@Override
	public String toString() {
		return "InputMapping{" + "targetParameter='" + targetParameter + '\'' + ", source=" + source + ", sourceKey='"
				+ sourceKey + '\'' + ", staticValue=" + staticValue + '}';
	}
}
