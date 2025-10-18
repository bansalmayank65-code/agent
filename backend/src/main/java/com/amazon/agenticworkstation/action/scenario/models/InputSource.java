package com.amazon.agenticworkstation.action.scenario.models;

/**
 * Enum representing the source of input for an action parameter.
 */
public enum InputSource {
    /**
     * Input comes from the scenario's input map (requiredInputsForScenario)
     */
    SCENARIO_INPUT,

    /**
     * Input comes from the output of a previous step in the scenario
     */
    PREVIOUS_STEP,

    /**
     * Input is a static value defined in the scenario configuration
     */
    STATIC_VALUE
}
