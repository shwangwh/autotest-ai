package com.testplatform.service;

import com.testplatform.dto.ProjectSummary;
import com.testplatform.entity.Project;
import com.testplatform.repository.ExecutionTaskRepository;
import com.testplatform.repository.ProjectRepository;
import com.testplatform.repository.RequirementRepository;
import com.testplatform.repository.TestCaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final TestCaseRepository testCaseRepository;
    private final ExecutionTaskRepository executionTaskRepository;

    public ProjectService(
        ProjectRepository projectRepository,
        RequirementRepository requirementRepository,
        TestCaseRepository testCaseRepository,
        ExecutionTaskRepository executionTaskRepository
    ) {
        this.projectRepository = projectRepository;
        this.requirementRepository = requirementRepository;
        this.testCaseRepository = testCaseRepository;
        this.executionTaskRepository = executionTaskRepository;
    }

    public List<ProjectSummary> getAllProjects() {
        return projectRepository.findAllOrderByCreatedAtDesc().stream()
            .map(this::toSummary)
            .toList();
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id).orElse(null);
    }

    public Project createProject(Project project) {
        if (isBlank(project.getOwner())) {
            project.setOwner("admin");
        }
        if (isBlank(project.getStatus())) {
            project.setStatus("ACTIVE");
        }
        if (isBlank(project.getReviewStatus())) {
            project.setReviewStatus("UNREVIEWED");
        }
        return projectRepository.save(project);
    }

    public Project updateProject(Long id, Project project) {
        Project existingProject = projectRepository.findById(id).orElse(null);
        if (existingProject != null) {
            existingProject.setName(project.getName());
            existingProject.setDescription(project.getDescription());
            if (!isBlank(project.getOwner())) {
                existingProject.setOwner(project.getOwner());
            }
            if (!isBlank(project.getStatus())) {
                existingProject.setStatus(project.getStatus());
            }
            if (!isBlank(project.getReviewStatus())) {
                existingProject.setReviewStatus(project.getReviewStatus());
            }
            return projectRepository.save(existingProject);
        }
        return null;
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    private ProjectSummary toSummary(Project project) {
        return new ProjectSummary(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getOwner(),
            project.getStatus(),
            project.getReviewStatus(),
            project.getCreatedAt(),
            requirementRepository.findByProjectId(project.getId()).size(),
            testCaseRepository.findByProjectId(project.getId()).size(),
            executionTaskRepository.findByProjectId(project.getId()).size()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
