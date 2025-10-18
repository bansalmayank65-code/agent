package com.amazon.agenticworkstation.action.scenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.agenticworkstation.action.scenario.models.ScenarioConfig;
import com.amazon.agenticworkstation.action.scenario.models.ScenarioStep;

/**
 * Service for merging multiple scenarios into a single combined workflow.
 * 
 * This service allows combining actions from multiple scenarios while removing
 * exact duplicates. This is useful for creating composite workflows or analyzing
 * scenario similarities.
 * 
 * Example usage:
 * <pre>
 * List&lt;String&gt; scenarioNames = List.of("UserProvisioningScenario", "CreateDepartmentScenario");
 * ScenarioMergerResult result = ScenarioMergerService.mergeScenarios(
 *     scenarioNames, "hr_experts", 1, new HashMap&lt;&gt;()
 * );
 * List&lt;ScenarioStep&gt; combinedActions = result.getMergedSteps();
 * </pre>
 */
public final class ScenarioMergerService {
    private static final Logger log = LoggerFactory.getLogger(ScenarioMergerService.class);
    
    private ScenarioMergerService() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Merge multiple scenarios into a single list of steps with duplicates removed.
     * 
     * @param scenarioNames List of scenario class simple names (e.g., "UserProvisioningScenario")
     * @param environment The environment name (e.g., "hr_experts")
     * @param interfaceNumber The interface number (1-5)
     * @param parameters Empty map for instantiating scenarios (scenarios are instantiated only to get configs)
     * @return ScenarioMergerResult containing merged steps and metadata
     * @throws IllegalArgumentException if any scenario is not found or parameters are invalid
     */
    public static ScenarioMergerResult mergeScenarios(
            List<String> scenarioNames,
            String environment,
            int interfaceNumber,
            Map<String, Object> parameters) {
        
        if (scenarioNames == null || scenarioNames.isEmpty()) {
            throw new IllegalArgumentException("Scenario names list cannot be null or empty");
        }
        
        if (environment == null || environment.isEmpty()) {
            throw new IllegalArgumentException("Environment cannot be null or empty");
        }
        
        if (interfaceNumber < 1 || interfaceNumber > 5) {
            throw new IllegalArgumentException("Interface number must be between 1 and 5");
        }
        
        log.info("Merging {} scenarios from environment '{}' interface {}", 
                scenarioNames.size(), environment, interfaceNumber);
        
        long startTime = System.currentTimeMillis();
        
        // Track order and duplicates
        List<ScenarioStep> mergedSteps = new ArrayList<>();
        Map<String, ScenarioStepInfo> stepIndex = new LinkedHashMap<>(); // Key = step signature
        List<String> processedScenarios = new ArrayList<>();
        int totalStepsBeforeMerge = 0;
        int duplicatesRemoved = 0;
        
        // Process each scenario
        for (String scenarioName : scenarioNames) {
            try {
                log.debug("Processing scenario: {}", scenarioName);
                
                // Get scenario class from registry
                Class<? extends BaseScenario> scenarioClass = Scenarios.getScenarioClass(
                        scenarioName, environment, interfaceNumber);
                
                // Create instance to get configuration
                BaseScenario scenario = createScenarioInstance(scenarioClass, parameters);
                ScenarioConfig config = scenario.getScenarioConfig();
                
                log.debug("Scenario '{}' has {} steps", scenarioName, config.getSteps().size());
                
                // Process each step from this scenario
                for (ScenarioStep step : config.getSteps()) {
                    totalStepsBeforeMerge++;
                    
                    // Generate unique signature for this step
                    String signature = generateStepSignature(step);
                    
                    if (stepIndex.containsKey(signature)) {
                        // Duplicate found - track it
                        duplicatesRemoved++;
                        ScenarioStepInfo existingInfo = stepIndex.get(signature);
                        existingInfo.addSourceScenario(scenarioName);
                        
                        log.debug("Duplicate step found: '{}' (action: '{}') - already exists from scenario(s): {}",
                                step.getStepId(), step.getActionName(), existingInfo.getSourceScenarios());
                    } else {
                        // New unique step - add it
                        mergedSteps.add(step);
                        ScenarioStepInfo info = new ScenarioStepInfo(step, scenarioName);
                        stepIndex.put(signature, info);
                        
                        log.debug("Added unique step: '{}' (action: '{}')", 
                                step.getStepId(), step.getActionName());
                    }
                }
                
                processedScenarios.add(scenarioName);
                
            } catch (Exception e) {
                log.error("Failed to process scenario '{}': {}", scenarioName, e.getMessage(), e);
                throw new IllegalArgumentException(
                        "Failed to process scenario '" + scenarioName + "': " + e.getMessage(), e);
            }
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        log.info("Scenario merge completed in {} ms: {} total steps -> {} unique steps ({} duplicates removed)",
                executionTime, totalStepsBeforeMerge, mergedSteps.size(), duplicatesRemoved);
        
        return new ScenarioMergerResult(
                processedScenarios,
                environment,
                interfaceNumber,
                mergedSteps,
                totalStepsBeforeMerge,
                duplicatesRemoved,
                executionTime,
                new ArrayList<>(stepIndex.values())
        );
    }
    
    /**
     * Generate a unique signature for a step to detect exact duplicates.
     * 
     * A duplicate is defined as having:
     * - Same action name
     * - Same input mappings (source, sourceKey, targetParameter, staticValue)
     * - Same description
     * 
     * Note: stepId is NOT included in the signature, as different scenarios
     * may use different step IDs for the same logical operation.
     */
    private static String generateStepSignature(ScenarioStep step) {
        StringBuilder signature = new StringBuilder();
        
        // Action name
        signature.append("action:").append(step.getActionName()).append("|");
        
        // Input mappings - sorted by target parameter for consistency
        List<String> mappingSignatures = step.getInputMappings().stream()
                .map(m -> String.format("mapping[%s->%s:%s:%s]",
                        m.getSource(),
                        m.getTargetParameter(),
                        m.getSourceKey() != null ? m.getSourceKey() : "",
                        m.getStaticValue() != null ? m.getStaticValue() : ""))
                .sorted()
                .collect(Collectors.toList());
        
        signature.append("mappings:").append(String.join(",", mappingSignatures)).append("|");
        
        // Description
        signature.append("desc:").append(step.getDescription() != null ? step.getDescription() : "");
        
        return signature.toString();
    }
    
    /**
     * Create an instance of a scenario class.
     */
    private static BaseScenario createScenarioInstance(
            Class<? extends BaseScenario> scenarioClass,
            Map<String, Object> parameters) {
        try {
            return scenarioClass.getConstructor(Map.class).newInstance(parameters);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to create instance of scenario: " + scenarioClass.getSimpleName() +
                    ". Ensure the scenario has a public constructor that accepts Map<String, Object>", e);
        }
    }
    
    /**
     * Result of merging multiple scenarios.
     */
    public static class ScenarioMergerResult {
        private final List<String> sourceScenarios;
        private final String environment;
        private final int interfaceNumber;
        private final List<ScenarioStep> mergedSteps;
        private final int totalStepsBeforeMerge;
        private final int duplicatesRemoved;
        private final long executionTimeMs;
        private final List<ScenarioStepInfo> stepDetails;
        
        public ScenarioMergerResult(
                List<String> sourceScenarios,
                String environment,
                int interfaceNumber,
                List<ScenarioStep> mergedSteps,
                int totalStepsBeforeMerge,
                int duplicatesRemoved,
                long executionTimeMs,
                List<ScenarioStepInfo> stepDetails) {
            this.sourceScenarios = sourceScenarios;
            this.environment = environment;
            this.interfaceNumber = interfaceNumber;
            this.mergedSteps = mergedSteps;
            this.totalStepsBeforeMerge = totalStepsBeforeMerge;
            this.duplicatesRemoved = duplicatesRemoved;
            this.executionTimeMs = executionTimeMs;
            this.stepDetails = stepDetails;
        }
        
        public List<String> getSourceScenarios() {
            return new ArrayList<>(sourceScenarios);
        }
        
        public String getEnvironment() {
            return environment;
        }
        
        public int getInterfaceNumber() {
            return interfaceNumber;
        }
        
        public List<ScenarioStep> getMergedSteps() {
            return new ArrayList<>(mergedSteps);
        }
        
        public int getTotalStepsBeforeMerge() {
            return totalStepsBeforeMerge;
        }
        
        public int getDuplicatesRemoved() {
            return duplicatesRemoved;
        }
        
        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
        
        public List<ScenarioStepInfo> getStepDetails() {
            return new ArrayList<>(stepDetails);
        }
        
        /**
         * Get a summary of the merge operation.
         */
        public String getSummary() {
            return String.format(
                    "Merged %d scenarios (%s) from environment '%s' interface %d: " +
                    "%d total steps -> %d unique steps (%d duplicates removed) in %d ms",
                    sourceScenarios.size(),
                    String.join(", ", sourceScenarios),
                    environment,
                    interfaceNumber,
                    totalStepsBeforeMerge,
                    mergedSteps.size(),
                    duplicatesRemoved,
                    executionTimeMs
            );
        }
        
        @Override
        public String toString() {
            return "ScenarioMergerResult{" +
                    "sourceScenarios=" + sourceScenarios +
                    ", environment='" + environment + '\'' +
                    ", interfaceNumber=" + interfaceNumber +
                    ", mergedSteps=" + mergedSteps.size() +
                    ", totalStepsBeforeMerge=" + totalStepsBeforeMerge +
                    ", duplicatesRemoved=" + duplicatesRemoved +
                    ", executionTimeMs=" + executionTimeMs +
                    '}';
        }
    }
    
    /**
     * Information about a step in the merged result, including which scenarios it came from.
     */
    public static class ScenarioStepInfo {
        private final ScenarioStep step;
        private final List<String> sourceScenarios;
        
        public ScenarioStepInfo(ScenarioStep step, String firstSourceScenario) {
            this.step = step;
            this.sourceScenarios = new ArrayList<>();
            this.sourceScenarios.add(firstSourceScenario);
        }
        
        public void addSourceScenario(String scenarioName) {
            if (!sourceScenarios.contains(scenarioName)) {
                sourceScenarios.add(scenarioName);
            }
        }
        
        public ScenarioStep getStep() {
            return step;
        }
        
        public List<String> getSourceScenarios() {
            return new ArrayList<>(sourceScenarios);
        }
        
        public boolean isDuplicate() {
            return sourceScenarios.size() > 1;
        }
        
        @Override
        public String toString() {
            return "ScenarioStepInfo{" +
                    "stepId='" + step.getStepId() + '\'' +
                    ", actionName='" + step.getActionName() + '\'' +
                    ", sourceScenarios=" + sourceScenarios +
                    ", isDuplicate=" + isDuplicate() +
                    '}';
        }
    }
}
