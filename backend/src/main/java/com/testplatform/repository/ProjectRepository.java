package com.testplatform.repository;

import com.testplatform.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query(value = "SELECT * FROM project ORDER BY created_at DESC", nativeQuery = true)
    List<Project> findAllOrderByCreatedAtDesc();
}
