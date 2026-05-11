package com.testplatform.repository;

import com.testplatform.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {
    List<Requirement> findByProjectId(Long projectId);
    List<Requirement> findByProjectIdAndTestType(Long projectId, String testType);
}
