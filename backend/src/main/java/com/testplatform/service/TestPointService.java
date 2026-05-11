package com.testplatform.service;

import com.testplatform.entity.Requirement;
import com.testplatform.entity.TestPoint;
import com.testplatform.repository.RequirementRepository;
import com.testplatform.repository.TestPointRepository;
import com.testplatform.util.TestMetadata;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TestPointService {
    private final TestPointRepository testPointRepository;
    private final RequirementRepository requirementRepository;

    public TestPointService(TestPointRepository testPointRepository, RequirementRepository requirementRepository) {
        this.testPointRepository = testPointRepository;
        this.requirementRepository = requirementRepository;
    }

    public List<TestPoint> getByRequirementId(Long requirementId) {
        List<TestPoint> testPoints = testPointRepository.findByRequirementId(requirementId);
        testPoints.forEach(this::normalizeTestPoint);
        return testPoints;
    }

    public List<TestPoint> getByProjectId(Long projectId) {
        List<Requirement> requirements = requirementRepository.findByProjectId(projectId);
        List<TestPoint> testPoints = new ArrayList<>();
        for (Requirement requirement : requirements) {
            testPoints.addAll(testPointRepository.findByRequirementId(requirement.getId()));
        }
        testPoints.forEach(this::normalizeTestPoint);
        return testPoints;
    }

    public List<TestPoint> getAllTestPoints() {
        List<TestPoint> testPoints = testPointRepository.findAll();
        testPoints.forEach(this::normalizeTestPoint);
        return testPoints;
    }

    public TestPoint createTestPoint(TestPoint testPoint) {
        // 生成测试点编码
        String pointCode = generatePointCode(testPoint.getProjectId());
        testPoint.setPointCode(pointCode);
        testPoint.setPointName(testPoint.getName());
        Requirement requirement = testPoint.getRequirementId() == null
                ? null
                : requirementRepository.findById(testPoint.getRequirementId()).orElse(null);
        String resolvedTestType = requirement != null ? requirement.getTestType() : testPoint.getTestType();
        testPoint.setPointType(TestMetadata.normalizeTestType(resolvedTestType));
        testPoint.setTestType(resolvedTestType);
        testPoint.setSourceType(TestMetadata.POINT_SOURCE_MANUAL_CREATED);
        testPoint.setFunction("测试");
        testPoint.setBusinessRule("Default rule created by user");
        testPoint.setCreatedAt(LocalDateTime.now());
        testPoint.setCreatedBy(1L);

        return testPointRepository.save(testPoint);
    }

    private String generatePointCode(Long projectId) {
        // 生成测试点编码，格式为TP-{projectId}-{sequence}
        long count = testPointRepository.countByProjectId(projectId) + 1;
        return String.format("TP-%d-%d", projectId, count);
    }

    public TestPoint updateTestPoint(Long id, TestPoint testPoint) {
        TestPoint existingTestPoint = testPointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test point not found with id: " + id));

        existingTestPoint.setName(testPoint.getName());
        existingTestPoint.setRequirementId(testPoint.getRequirementId());
        existingTestPoint.setSceneType(testPoint.getSceneType());
        existingTestPoint.setRiskLevel(testPoint.getRiskLevel());
        existingTestPoint.setDescription(testPoint.getDescription());
        existingTestPoint.setPointName(testPoint.getName());
        existingTestPoint.setTestType(testPoint.getTestType());
        existingTestPoint.setPointType(existingTestPoint.getTestType());
        existingTestPoint.setSourceType(testPoint.getSourceType());

        return testPointRepository.save(existingTestPoint);
    }

    public void deleteTestPoint(Long id) {
        TestPoint existingTestPoint = testPointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Test point not found with id: " + id));
        testPointRepository.delete(existingTestPoint);
    }

    private void normalizeTestPoint(TestPoint testPoint) {
        if (testPoint == null) {
            return;
        }
        if (testPoint.getTestType() == null && testPoint.getRequirementId() != null) {
            Requirement requirement = requirementRepository.findById(testPoint.getRequirementId()).orElse(null);
            if (requirement != null) {
                testPoint.setTestType(requirement.getTestType());
            }
        }
        testPoint.setTestType(testPoint.getTestType());
        testPoint.setPointType(testPoint.getPointType() == null ? testPoint.getTestType() : testPoint.getPointType());
        testPoint.setSourceType(testPoint.getSourceType());
    }
}
