package com.amazon.agenticworkstation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity for task management
 */
@Entity
@Table(name = "task")
public class TaskEntity {
    
    @Id
    @Column(name = "task_id", length = 100)
    @NotBlank(message = "Task ID is required")
    @Size(max = 100, message = "Task ID must not exceed 100 characters")
    private String taskId;
    
    @Column(name = "env_name", length = 100, nullable = false)
    @NotBlank(message = "Environment name is required")
    @Size(max = 100, message = "Environment name must not exceed 100 characters")
    private String envName;
    
    @Column(name = "interface_num", nullable = false)
    @NotNull(message = "Interface number is required")
    private Integer interfaceNum;
    
    @Column(name = "instruction", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Instruction is required")
    private String instruction;
    
    @Column(name = "num_of_edges")
    private Integer numOfEdges = 0;
    
    @Column(name = "task_json", columnDefinition = "TEXT")
    private String taskJson;
    
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;
    
    @Column(name = "user_id", length = 50, nullable = false)
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "task_status")
    private TaskStatus taskStatus = TaskStatus.DRAFT;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_date_time")
    private LocalDateTime createdDateTime;
    
    @Column(name = "updated_date_time")
    private LocalDateTime updatedDateTime;
    
    // Enum for task status
    public enum TaskStatus {
        DRAFT, DISCARDED, SUBMITTED, APPROVED, REJECTED, NEEDS_CHANGES, MERGED
    }
    
    // Constructors
    public TaskEntity() {}
    
    public TaskEntity(String taskId, String envName, Integer interfaceNum, String instruction, String userId) {
        this.taskId = taskId;
        this.envName = envName;
        this.interfaceNum = interfaceNum;
        this.instruction = instruction;
        this.userId = userId;
        this.taskStatus = TaskStatus.DRAFT;
        this.isActive = true;
        this.numOfEdges = 0;
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getEnvName() {
        return envName;
    }
    
    public void setEnvName(String envName) {
        this.envName = envName;
    }
    
    public Integer getInterfaceNum() {
        return interfaceNum;
    }
    
    public void setInterfaceNum(Integer interfaceNum) {
        this.interfaceNum = interfaceNum;
    }
    
    public String getInstruction() {
        return instruction;
    }
    
    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
    
    public Integer getNumOfEdges() {
        return numOfEdges;
    }
    
    public void setNumOfEdges(Integer numOfEdges) {
        this.numOfEdges = numOfEdges;
    }
    
    public String getTaskJson() {
        return taskJson;
    }
    
    public void setTaskJson(String taskJson) {
        this.taskJson = taskJson;
    }
    
    public String getResultJson() {
        return resultJson;
    }
    
    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public TaskStatus getTaskStatus() {
        return taskStatus;
    }
    
    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedDateTime() {
        return createdDateTime;
    }
    
    public void setCreatedDateTime(LocalDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }
    
    public LocalDateTime getUpdatedDateTime() {
        return updatedDateTime;
    }
    
    public void setUpdatedDateTime(LocalDateTime updatedDateTime) {
        this.updatedDateTime = updatedDateTime;
    }
    
    @PrePersist
    protected void onCreate() {
        this.createdDateTime = LocalDateTime.now();
        this.updatedDateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedDateTime = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "TaskEntity{" +
                "taskId='" + taskId + '\'' +
                ", envName='" + envName + '\'' +
                ", interfaceNum=" + interfaceNum +
                ", userId='" + userId + '\'' +
                ", taskStatus=" + taskStatus +
                ", isActive=" + isActive +
                '}';
    }
}