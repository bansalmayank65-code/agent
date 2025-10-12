package com.amazon.agenticworkstation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity for login history tracking
 */
@Entity
@Table(name = "login_history")
public class LoginHistoryEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_history_id")
    private Long loginHistoryId;
    
    @Column(name = "user_id", length = 50, nullable = false)
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @Column(name = "login_timestamp")
    private LocalDateTime loginTimestamp;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "login_status", nullable = false)
    private LoginStatus loginStatus;
    
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    // Enum for login status
    public enum LoginStatus {
        SUCCESS, FAILED, LOGOUT
    }
    
    // Constructors
    public LoginHistoryEntity() {}
    
    public LoginHistoryEntity(String userId, LoginStatus loginStatus) {
        this.userId = userId;
        this.loginStatus = loginStatus;
        this.loginTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getLoginHistoryId() {
        return loginHistoryId;
    }
    
    public void setLoginHistoryId(Long loginHistoryId) {
        this.loginHistoryId = loginHistoryId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getLoginTimestamp() {
        return loginTimestamp;
    }
    
    public void setLoginTimestamp(LocalDateTime loginTimestamp) {
        this.loginTimestamp = loginTimestamp;
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
    
    public LoginStatus getLoginStatus() {
        return loginStatus;
    }
    
    public void setLoginStatus(LoginStatus loginStatus) {
        this.loginStatus = loginStatus;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @PrePersist
    protected void onCreate() {
        this.loginTimestamp = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "LoginHistoryEntity{" +
                "loginHistoryId=" + loginHistoryId +
                ", userId='" + userId + '\'' +
                ", loginTimestamp=" + loginTimestamp +
                ", loginStatus=" + loginStatus +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}