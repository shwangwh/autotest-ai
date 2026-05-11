package com.testplatform.repository;

import com.testplatform.entity.ExecutionResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionResultRepository extends JpaRepository<ExecutionResult, Long> {
    ExecutionResult findByTaskId(Long taskId);
}
