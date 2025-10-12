package com.amazon.agenticworkstation.dto;

import java.util.List;

/**
 * DTO for comprehensive task validation response
 * Includes results from all 4 validation steps
 */
public class ComprehensiveValidationDto {
    private TaskValidationResultDto complexityResult;
    private TaskValidationResultDto verificationResult;
    private TaskValidationResultDto runTaskResult;
    private TaskValidationResultDto evaluationResult;
    
    private boolean allStepsSuccessful;
    private String overallMessage;
    private List<String> generatedFiles;

    // Default constructor
    public ComprehensiveValidationDto() {}

    // Getters and setters
    public TaskValidationResultDto getComplexityResult() { return complexityResult; }
    public void setComplexityResult(TaskValidationResultDto complexityResult) { this.complexityResult = complexityResult; }

    public TaskValidationResultDto getVerificationResult() { return verificationResult; }
    public void setVerificationResult(TaskValidationResultDto verificationResult) { this.verificationResult = verificationResult; }

    public TaskValidationResultDto getRunTaskResult() { return runTaskResult; }
    public void setRunTaskResult(TaskValidationResultDto runTaskResult) { this.runTaskResult = runTaskResult; }

    public TaskValidationResultDto getEvaluationResult() { return evaluationResult; }
    public void setEvaluationResult(TaskValidationResultDto evaluationResult) { this.evaluationResult = evaluationResult; }

    public boolean isAllStepsSuccessful() { return allStepsSuccessful; }
    public void setAllStepsSuccessful(boolean allStepsSuccessful) { this.allStepsSuccessful = allStepsSuccessful; }

    public String getOverallMessage() { return overallMessage; }
    public void setOverallMessage(String overallMessage) { this.overallMessage = overallMessage; }

    public List<String> getGeneratedFiles() { return generatedFiles; }
    public void setGeneratedFiles(List<String> generatedFiles) { this.generatedFiles = generatedFiles; }
}