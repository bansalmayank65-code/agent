package com.amazon.agenticworkstation.action.scenario.models;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for a scenario containing multiple actions to be executed in sequence.
 * Each scenario represents a predefined workflow with specific actions and their dependencies.
 */
public class ScenarioConfig {
    private final String scenarioName;
    private final String description;
    private final String envName;
    private final int interfaceNumber;
    private final List<ScenarioStep> steps;

    private ScenarioConfig(Builder builder) {
        this.scenarioName = Objects.requireNonNull(builder.scenarioName, "scenarioName cannot be null");
        this.description = builder.description;
        this.envName = Objects.requireNonNull(builder.envName, "envName cannot be null");
        this.interfaceNumber = builder.interfaceNumber;
        this.steps = Objects.requireNonNull(builder.steps, "steps cannot be null");
        
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Scenario must have at least one step");
        }
        if (interfaceNumber < 1 || interfaceNumber > 5) {
            throw new IllegalArgumentException("Interface number must be between 1 and 5");
        }
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getDescription() {
        return description;
    }

    public String getEnvName() {
        return envName;
    }

    public int getInterfaceNumber() {
        return interfaceNumber;
    }

    public List<ScenarioStep> getSteps() {
        return steps;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String scenarioName;
        private String description;
        private String envName;
        private int interfaceNumber;
        private List<ScenarioStep> steps;

        public Builder scenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder envName(String envName) {
            this.envName = envName;
            return this;
        }

        public Builder interfaceNumber(int interfaceNumber) {
            this.interfaceNumber = interfaceNumber;
            return this;
        }

        public Builder steps(List<ScenarioStep> steps) {
            this.steps = steps;
            return this;
        }

        public ScenarioConfig build() {
            return new ScenarioConfig(this);
        }
    }

    @Override
    public String toString() {
        return "ScenarioConfig{" +
                "scenarioName='" + scenarioName + '\'' +
                ", description='" + description + '\'' +
                ", envName='" + envName + '\'' +
                ", interfaceNumber=" + interfaceNumber +
                ", steps=" + steps.size() +
                '}';
    }
}
