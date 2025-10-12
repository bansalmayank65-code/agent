package com.amazon.agenticworkstation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity for task history tracking
 */
@Entity
@Table(name = "task_history")
public class TaskHistoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;
    
    @Column(name = "task_id", length = 100, nullable = false)
    @NotBlank(message = "Task ID is required")
    private String taskId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    @NotNull(message = "Action type is required")
    private ActionType actionType;
    
    @Column(name = "old_values", columnDefinition = "JSONB")
    private String oldValues;
    
    @Column(name = "new_values", columnDefinition = "JSONB")
    private String newValues;
    
    @Column(name = "changed_by", length = 50, nullable = false)
    @NotBlank(message = "Changed by is required")
    private String changedBy;
    
    @Column(name = "change_timestamp")
    private LocalDateTime changeTimestamp;
    
    @Column(name = "change_reason", length = 500)
    @Size(max = 500, message = "Change reason must not exceed 500 characters")
    private String changeReason;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    // Enum for action types
    public enum ActionType {
        CREATED, UPDATED, STATUS_CHANGED, DELETED
    }
    
    // Constructors
    public TaskHistoryEntity() {}
    
    public TaskHistoryEntity(String taskId, ActionType actionType, String changedBy) {
        this.taskId = taskId;
        this.actionType = actionType;
        this.changedBy = changedBy;
        this.changeTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getHistoryId() {
        return historyId;
    }
    
    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }
    
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    
    public String getOldValues() {
        return oldValues;
    }
    
    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }
    
    public String getNewValues() {
        return newValues;
    }
    
    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }
    
    public String getChangedBy() {
        return changedBy;
    }
    
    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
    
    public LocalDateTime getChangeTimestamp() {
        return changeTimestamp;
    }
    
    public void setChangeTimestamp(LocalDateTime changeTimestamp) {
        this.changeTimestamp = changeTimestamp;
    }
    
    public String getChangeReason() {
        return changeReason;
    }
    
    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    @PrePersist
    protected void onCreate() {
        this.changeTimestamp = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "TaskHistoryEntity{" +
                "historyId=" + historyId +
                ", taskId='" + taskId + '\'' +
                ", actionType=" + actionType +
                ", changedBy='" + changedBy + '\'' +
                ", changeTimestamp=" + changeTimestamp +
                '}';
    }
}