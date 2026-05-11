package com.testplatform.repository;

import com.testplatform.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByProjectId(Long projectId);
    List<TestCase> findByProjectIdAndTestType(Long projectId, String testType);
    List<TestCase> findByProjectIdAndExecutionType(Long projectId, String executionType);
    List<TestCase> findByTestPointId(Long testPointId);
}
