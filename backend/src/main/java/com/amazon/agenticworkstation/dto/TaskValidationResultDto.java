package com.amazon.agenticworkstation.dto;

import java.util.Map;

/**
 * DTO for task validation results from the tau-bench API
 */
public class TaskValidationResultDto {
    private String endpoint;
    private boolean success;
    private String message;
    private Map<String, Object> responseData;
    private String errorDetails;

    // Default constructor
    public TaskValidationResultDto() {}

    // Constructor
    public TaskValidationResultDto(String endpoint, boolean success, String message) {
        this.endpoint = endpoint;
        this.success = success;
        this.message = message;
    }

    // Getters and setters
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getResponseData() { return responseData; }
    public void setResponseData(Map<String, Object> responseData) { this.responseData = responseData; }

    public String getErrorDetails() { return errorDetails; }
    public void setErrorDetails(String errorDetails) { this.errorDetails = errorDetails; }
}