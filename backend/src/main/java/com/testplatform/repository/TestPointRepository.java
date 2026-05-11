package com.testplatform.repository;

import com.testplatform.entity.TestPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestPointRepository extends JpaRepository<TestPoint, Long> {
    List<TestPoint> findByRequirementId(Long requirementId);
    List<TestPoint> findByRequirementIdAndTestType(Long requirementId, String testType);
    List<TestPoint> findByProjectIdAndTestType(Long projectId, String testType);
    long countByProjectId(Long projectId);
}
