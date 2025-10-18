package com.amazon.agenticworkstation.action.scenario.models;

import java.util.Map;

/**
 * Interface for scenario input definitions.
 * 
 * This interface defines the required inputs that a scenario needs from the UI.
 * Each scenario implementation should provide this information to help the UI
 * dynamically generate input forms.
 */
public class ScenarioInputDefinition {
	private final String name;
	private final String type;
	private final String description;
	private final boolean required;
	private final String defaultValue;
	private final String example;

	private ScenarioInputDefinition(Builder builder) {
		this.name = builder.name;
		this.type = builder.type;
		this.description = builder.description;
		this.required = builder.required;
		this.defaultValue = builder.defaultValue;
		this.example = builder.example;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public boolean isRequired() {
		return required;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getExample() {
		return example;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private String type = "string"; // default type
		private String description;
		private boolean required = true; // default required
		private String defaultValue;
		private String example;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder type(String type) {
			this.type = type;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public Builder required(boolean required) {
			this.required = required;
			return this;
		}

		public Builder defaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

		public Builder example(String example) {
			this.example = example;
			return this;
		}

		public ScenarioInputDefinition build() {
			return new ScenarioInputDefinition(this);
		}
	}

	@Override
	public String toString() {
		return "ScenarioInputDefinition{" + "name='" + name + '\'' + ", type='" + type + '\'' + ", description='"
				+ description + '\'' + ", required=" + required + ", defaultValue='" + defaultValue + '\''
				+ ", example='" + example + '\'' + '}';
	}

	/**
	 * Convert to a Map representation suitable for JSON serialization
	 */
	public Map<String, Object> toMap() {
		return Map.of("name", name, "type", type, "description", description != null ? description : "", "required",
				required, "defaultValue", defaultValue != null ? defaultValue : "", "example",
				example != null ? example : "");
	}
}
