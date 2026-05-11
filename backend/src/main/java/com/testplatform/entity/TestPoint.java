package com.testplatform.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import com.testplatform.util.TestMetadata;
import java.time.LocalDateTime;

@Entity
public class TestPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long projectId;
    private Long requirementId;
    private Long requirementItemId;
    private String pointCode;
    private String pointName;
    private String pointType;
    private String testType;
    private String sourceType;
    private String name;
    private String function;
    private String sceneType;
    private String riskLevel;
    private String businessRule;
    private String description;
    private Boolean automationSuggested;
    private LocalDateTime createdAt;
    private Long createdBy;

    // 构造方法、getter和setter
    public TestPoint() {
        this.createdAt = LocalDateTime.now();
        this.automationSuggested = false;
        this.createdBy = 0L;
        this.testType = TestMetadata.TEST_TYPE_INTERFACE;
        this.sourceType = TestMetadata.POINT_SOURCE_MIGRATED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(Long requirementId) {
        this.requirementId = requirementId;
    }

    public Long getRequirementItemId() {
        return requirementItemId;
    }

    public void setRequirementItemId(Long requirementItemId) {
        this.requirementItemId = requirementItemId;
    }

    public String getPointCode() {
        return pointCode;
    }

    public void setPointCode(String pointCode) {
        this.pointCode = pointCode;
    }

    public String getPointName() {
        return pointName;
    }

    public void setPointName(String pointName) {
        this.pointName = pointName;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = TestMetadata.normalizeTestType(testType);
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = TestMetadata.normalizePointSourceType(sourceType);
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getBusinessRule() {
        return businessRule;
    }

    public void setBusinessRule(String businessRule) {
        this.businessRule = businessRule;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getAutomationSuggested() {
        return automationSuggested;
    }

    public void setAutomationSuggested(Boolean automationSuggested) {
        this.automationSuggested = automationSuggested;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
