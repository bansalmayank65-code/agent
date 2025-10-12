package com.amazon.agenticworkstation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity for basic authentication
 */
@Entity
@Table(name = "login")
public class LoginEntity {
    
    @Id
    @Column(name = "user_id", length = 50)
    @NotBlank(message = "User ID is required")
    @Size(max = 50, message = "User ID must not exceed 50 characters")
    private String userId;
    
    @Column(name = "password", nullable = false)
    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Password must not exceed 255 characters")
    private String password;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "created_date_time")
    private LocalDateTime createdDateTime;
    
    @Column(name = "updated_date_time")
    private LocalDateTime updatedDateTime;
    
    // Constructors
    public LoginEntity() {}
    
    public LoginEntity(String userId, String password) {
        this.userId = userId;
        this.password = password;
        this.isActive = true;
        this.createdDateTime = LocalDateTime.now();
        this.updatedDateTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
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
        return "LoginEntity{" +
                "userId='" + userId + '\'' +
                ", isActive=" + isActive +
                ", createdDateTime=" + createdDateTime +
                ", updatedDateTime=" + updatedDateTime +
                '}';
    }
}