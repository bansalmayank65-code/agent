package com.amazon.agenticworkstation.action.scenario.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Metadata about a Python function extracted from get_info()
 */
public class PythonFunctionMetadata {
	private String functionName;
	private String description;
	private Map<String, ParameterInfo> parameters;
	private List<String> requiredParams;

	public static class ParameterInfo {
		private String type;
		private String description;
		private List<String> enumValues;
		private boolean required;

		public ParameterInfo() {
		}

		public ParameterInfo(String type, String description, boolean required) {
			this.type = type;
			this.description = description;
			this.required = required;
		}

		// Getters and setters
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public List<String> getEnumValues() {
			return enumValues;
		}

		public void setEnumValues(List<String> enumValues) {
			this.enumValues = enumValues;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}
	}

	public PythonFunctionMetadata() {
		this.requiredParams = new ArrayList<>();
	}

	// Getters and setters
	public String getFunctionName() {
		return functionName;
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, ParameterInfo> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, ParameterInfo> parameters) {
		this.parameters = parameters;
	}

	public List<String> getRequiredParams() {
		return requiredParams;
	}

	public void setRequiredParams(List<String> requiredParams) {
		this.requiredParams = requiredParams;
	}
}
