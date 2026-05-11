package com.testplatform.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import com.testplatform.util.TestMetadata;

import java.time.LocalDateTime;

@Entity
public class ExecutionTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long projectId;
    private Long testCaseId;
    @Column(nullable = true)
    private Long planId;
    private String taskNo;
    private String taskType;
    private String runnerType;
    private String environment;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;

    public ExecutionTask() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
        this.taskNo = "TASK-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        this.taskType = TestMetadata.TEST_TYPE_INTERFACE;
        this.runnerType = "LOCAL";
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

    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = TestMetadata.normalizeTestType(taskType);
    }

    public String getRunnerType() {
        return runnerType;
    }

    public void setRunnerType(String runnerType) {
        this.runnerType = runnerType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        if ("RUNNING".equals(status) || "COMPLETED".equals(status) || "FAILED".equals(status)) {
            this.executedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
}
