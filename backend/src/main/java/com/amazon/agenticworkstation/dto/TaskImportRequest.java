package com.amazon.agenticworkstation.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * DTO for task import requests
 */
public class TaskImportRequest {
    
    private String userId;
    private JsonNode taskJsonContent;
    
    // Default constructor
    public TaskImportRequest() {}
    
    // Constructor with parameters
    public TaskImportRequest(String userId, JsonNode taskJsonContent) {
        this.userId = userId;
        this.taskJsonContent = taskJsonContent;
    }
    
    // Getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public JsonNode getTaskJsonContent() {
        return taskJsonContent;
    }
    
    public void setTaskJsonContent(JsonNode taskJsonContent) {
        this.taskJsonContent = taskJsonContent;
    }
    
    @Override
    public String toString() {
        return "TaskImportRequest{" +
                "userId='" + userId + '\'' +
                ", taskJsonContent=" + taskJsonContent +
                '}';
    }
}