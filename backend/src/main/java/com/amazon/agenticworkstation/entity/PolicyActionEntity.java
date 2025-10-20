package com.amazon.agenticworkstation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Entity for policy actions management
 * Stores policy actions for different scenarios to build action plans
 */
@Entity
@Table(name = "policy_actions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_policy_actions_combination",
            columnNames = {"env_name", "interface_num", "policy_cat1", "policy_cat2"}
        )
    }
)
public class PolicyActionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_action_id")
    private Long policyActionId;
    
    @Column(name = "env_name", length = 100, nullable = false)
    @NotBlank(message = "Environment name is required")
    @Size(max = 100, message = "Environment name must not exceed 100 characters")
    private String envName;
    
    @Column(name = "interface_num", nullable = false)
    @NotNull(message = "Interface number is required")
    private Integer interfaceNum;
    
    @Column(name = "policy_cat1", length = 100, nullable = false)
    @NotBlank(message = "Policy category 1 is required")
    @Size(max = 100, message = "Policy category 1 must not exceed 100 characters")
    private String policyCat1;
    
    @Column(name = "policy_cat2", length = 100, nullable = false)
    @NotBlank(message = "Policy category 2 is required")
    @Size(max = 100, message = "Policy category 2 must not exceed 100 characters")
    private String policyCat2;
    
    @Column(name = "policy_description", columnDefinition = "TEXT")
    private String policyDescription;
    
    @Column(name = "actions_json", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Actions JSON is required")
    private String actionsJson;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
    
    // Constructors
    public PolicyActionEntity() {}
    
    public PolicyActionEntity(String envName, Integer interfaceNum, String policyCat1, 
                             String policyCat2, String policyDescription, String actionsJson) {
        this.envName = envName;
        this.interfaceNum = interfaceNum;
        this.policyCat1 = policyCat1;
        this.policyCat2 = policyCat2;
        this.policyDescription = policyDescription;
        this.actionsJson = actionsJson;
    }
    
    // Getters and Setters
    public Long getPolicyActionId() {
        return policyActionId;
    }
    
    public void setPolicyActionId(Long policyActionId) {
        this.policyActionId = policyActionId;
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
    
    public String getPolicyCat1() {
        return policyCat1;
    }
    
    public void setPolicyCat1(String policyCat1) {
        this.policyCat1 = policyCat1;
    }
    
    public String getPolicyCat2() {
        return policyCat2;
    }
    
    public void setPolicyCat2(String policyCat2) {
        this.policyCat2 = policyCat2;
    }
    
    public String getPolicyDescription() {
        return policyDescription;
    }
    
    public void setPolicyDescription(String policyDescription) {
        this.policyDescription = policyDescription;
    }
    
    public String getActionsJson() {
        return actionsJson;
    }
    
    public void setActionsJson(String actionsJson) {
        this.actionsJson = actionsJson;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "PolicyActionEntity{" +
                "policyActionId=" + policyActionId +
                ", envName='" + envName + '\'' +
                ", interfaceNum=" + interfaceNum +
                ", policyCat1='" + policyCat1 + '\'' +
                ", policyCat2='" + policyCat2 + '\'' +
                ", policyDescription='" + policyDescription + '\'' +
                '}';
    }
}
