package com.amazon.agenticworkstation.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * DTO for task import requests
 */
public class TaskImportRequest {
    
    private String userId;
    private JsonNode taskJsonContent;
    private String dbUserId; // For database foreign key constraint
    
    // Default constructor
    public TaskImportRequest() {}
    
    // Constructor with parameters
    public TaskImportRequest(String userId, JsonNode taskJsonContent) {
        this.userId = userId;
        this.taskJsonContent = taskJsonContent;
    }
    
    // Constructor with dbUserId
    public TaskImportRequest(String userId, JsonNode taskJsonContent, String dbUserId) {
        this.userId = userId;
        this.taskJsonContent = taskJsonContent;
        this.dbUserId = dbUserId;
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
    
    public String getDbUserId() {
        return dbUserId;
    }
    
    public void setDbUserId(String dbUserId) {
        this.dbUserId = dbUserId;
    }
    
    @Override
    public String toString() {
        return "TaskImportRequest{" +
                "userId='" + userId + '\'' +
                ", taskJsonContent=" + taskJsonContent +
                ", dbUserId='" + dbUserId + '\'' +
                '}';
    }
}