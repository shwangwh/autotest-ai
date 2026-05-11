package com.testplatform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import com.testplatform.util.TestMetadata;

import java.time.LocalDateTime;

@Entity
public class ExecutionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long taskId;
    private String testType;
    private String requestUrl;
    private String requestMethod;
    @Column(length = 1000)
    private String requestHeaders;
    @Column(length = 2000)
    private String requestBody;
    private int responseStatus;
    @Column(columnDefinition = "TEXT")
    private String responseBody;
    @Column(columnDefinition = "TEXT")
    private String assertionResult;
    @Column(columnDefinition = "TEXT")
    private String llmAnalysis;
    private String result;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    @Column(columnDefinition = "TEXT")
    private String artifacts;
    private String reportUrl;
    private LocalDateTime executedAt;

    public ExecutionResult() {
        this.executedAt = LocalDateTime.now();
        this.testType = TestMetadata.TEST_TYPE_INTERFACE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = TestMetadata.normalizeTestType(testType);
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getAssertionResult() {
        return assertionResult;
    }

    public void setAssertionResult(String assertionResult) {
        this.assertionResult = assertionResult;
    }

    public String getLlmAnalysis() {
        return llmAnalysis;
    }

    public void setLlmAnalysis(String llmAnalysis) {
        this.llmAnalysis = llmAnalysis;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(String artifacts) {
        this.artifacts = artifacts;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
}
