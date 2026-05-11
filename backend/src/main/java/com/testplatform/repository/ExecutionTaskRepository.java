package com.testplatform.repository;

import com.testplatform.entity.ExecutionTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionTaskRepository extends JpaRepository<ExecutionTask, Long> {
    List<ExecutionTask> findByProjectId(Long projectId);
    List<ExecutionTask> findByTestCaseId(Long testCaseId);
}
