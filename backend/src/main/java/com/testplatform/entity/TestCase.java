package com.testplatform.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import com.testplatform.util.TestMetadata;

import java.time.LocalDateTime;

@Entity
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long projectId;
    private Long testPointId;
    private String caseType;
    private String testType;
    private String executionType;
    private Long scriptRefId;
    private String caseNumber;
    private String caseCode;
    private String title;
    private String precondition;
    private String steps;
    private String expectedResult;
    private String priority;
    @Column(columnDefinition = "TEXT")
    private String automationPayload;
    private String requestData;
    private Boolean automation;
    private String status;
    private String creator;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TestCase() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.automation = false;
        this.status = "PENDING_REVIEW";
        this.priority = "MEDIUM";
        this.creator = "admin";
        this.createdBy = 1L;
        this.caseType = TestMetadata.TEST_TYPE_INTERFACE;
        this.testType = TestMetadata.TEST_TYPE_INTERFACE;
        this.executionType = TestMetadata.EXECUTION_TYPE_API_AUTOMATION;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTestPointId() {
        return testPointId;
    }

    public void setTestPointId(Long testPointId) {
        this.testPointId = testPointId;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = TestMetadata.normalizeTestType(caseType);
        touch();
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = TestMetadata.normalizeTestType(testType);
        touch();
    }

    public String getExecutionType() {
        return executionType;
    }

    public void setExecutionType(String executionType) {
        this.executionType = TestMetadata.normalizeExecutionType(executionType, this.testType);
        touch();
    }

    public Long getScriptRefId() {
        return scriptRefId;
    }

    public void setScriptRefId(Long scriptRefId) {
        this.scriptRefId = scriptRefId;
        touch();
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public String getPrecondition() {
        return precondition;
    }

    public void setPrecondition(String precondition) {
        this.precondition = precondition;
        touch();
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
        touch();
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
        touch();
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
        touch();
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
        touch();
    }

    public String getAutomationPayload() {
        return automationPayload;
    }

    public void setAutomationPayload(String automationPayload) {
        this.automationPayload = automationPayload;
        touch();
    }

    public Boolean getAutomation() {
        return automation;
    }

    public void setAutomation(Boolean automation) {
        this.automation = automation;
        touch();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
