package com.amazon.agenticworkstation.action.scenario.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single step in a scenario workflow.
 * Each step executes an action and can have dependencies on previous steps.
 */
public class ScenarioStep {
    private final String stepId;
    private final String actionName;
    private final List<InputMapping> inputMappings;
    private final String description;

    private ScenarioStep(Builder builder) {
        this.stepId = Objects.requireNonNull(builder.stepId, "stepId cannot be null");
        this.actionName = Objects.requireNonNull(builder.actionName, "actionName cannot be null");
        this.inputMappings = Objects.requireNonNull(builder.inputMappings, "inputMappings cannot be null");
        this.description = builder.description;
    }

    public String getStepId() {
        return stepId;
    }

    public String getActionName() {
        return actionName;
    }

    public List<InputMapping> getInputMappings() {
        return Collections.unmodifiableList(inputMappings);
    }

    public String getDescription() {
        return description;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String stepId;
        private String actionName;
        private List<InputMapping> inputMappings = new ArrayList<>();
        private String description;

        public Builder stepId(String stepId) {
            this.stepId = stepId;
            return this;
        }

        public Builder actionName(String actionName) {
            this.actionName = actionName;
            return this;
        }

        public Builder inputMappings(List<InputMapping> inputMappings) {
            this.inputMappings = new ArrayList<>(inputMappings);
            return this;
        }

        public Builder addInputMapping(InputMapping mapping) {
            this.inputMappings.add(mapping);
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public ScenarioStep build() {
            return new ScenarioStep(this);
        }
    }

    @Override
    public String toString() {
        return "ScenarioStep{" +
                "stepId='" + stepId + '\'' +
                ", actionName='" + actionName + '\'' +
                ", inputMappings=" + inputMappings.size() +
                ", description='" + description + '\'' +
                '}';
    }
}
