package com.testplatform.dto;

import java.time.LocalDateTime;

public class ProjectSummary {
    private final Long id;
    private final String name;
    private final String description;
    private final String owner;
    private final String status;
    private final String reviewStatus;
    private final LocalDateTime createdAt;
    private final int requirementCount;
    private final int caseCount;
    private final int taskCount;

    public ProjectSummary(
        Long id,
        String name,
        String description,
        String owner,
        String status,
        String reviewStatus,
        LocalDateTime createdAt,
        int requirementCount,
        int caseCount,
        int taskCount
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.status = status;
        this.reviewStatus = reviewStatus;
        this.createdAt = createdAt;
        this.requirementCount = requirementCount;
        this.caseCount = caseCount;
        this.taskCount = taskCount;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getOwner() {
        return owner;
    }

    public String getStatus() {
        return status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getRequirementCount() {
        return requirementCount;
    }

    public int getCaseCount() {
        return caseCount;
    }

    public int getTaskCount() {
        return taskCount;
    }
}
