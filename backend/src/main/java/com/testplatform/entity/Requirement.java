package com.testplatform.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import com.testplatform.util.TestMetadata;

import java.time.LocalDateTime;

@Entity
public class Requirement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long projectId;
    private String name;
    private String fileUrl;
    private String version;
    private String status;
    private String testType;
    private Integer testPointCount;
    private String owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Requirement() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = "v1.0";
        this.status = "PENDING_ANALYSIS";
        this.testType = TestMetadata.TEST_TYPE_INTERFACE;
        this.testPointCount = 0;
        this.owner = "admin";
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
        touch();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
        touch();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = TestMetadata.normalizeTestType(testType);
        touch();
    }

    public Integer getTestPointCount() {
        return testPointCount;
    }

    public void setTestPointCount(Integer testPointCount) {
        this.testPointCount = testPointCount;
        touch();
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
        touch();
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
