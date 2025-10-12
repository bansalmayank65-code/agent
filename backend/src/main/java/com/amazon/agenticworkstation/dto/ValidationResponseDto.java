package com.amazon.agenticworkstation.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for validation responses and results
 */
public class ValidationResponseDto {
    private boolean success;
    private String message;
    private List<String> errors;
    private List<String> warnings;
    private Map<String, Object> details;

    // Default constructor
    public ValidationResponseDto() {}

    // Constructor for success case
    public ValidationResponseDto(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Constructor with errors
    public ValidationResponseDto(boolean success, String message, List<String> errors) {
        this.success = success;
        this.message = message;
        this.errors = errors;
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}