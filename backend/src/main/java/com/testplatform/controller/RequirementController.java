package com.testplatform.controller;

import com.testplatform.entity.Requirement;
import com.testplatform.entity.TestPoint;
import com.testplatform.service.RequirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {
    @Autowired
    private RequirementService requirementService;

    @GetMapping
    public List<Requirement> getAllRequirements() {
        return requirementService.getAllRequirements();
    }

    @GetMapping("/project/{projectId}")
    public List<Requirement> getRequirementsByProjectId(@PathVariable Long projectId) {
        return requirementService.getRequirementsByProjectId(projectId);
    }

    @GetMapping("/{id}")
    public Requirement getRequirementById(@PathVariable Long id) {
        return requirementService.getRequirementById(id);
    }

    @PostMapping("/upload")
    public Requirement uploadRequirement(
            @RequestParam Long projectId,
            @RequestParam MultipartFile file,
            @RequestParam String name,
            @RequestParam String version,
            @RequestParam String testType
    ) throws IOException {
        return requirementService.uploadRequirement(projectId, file, name, version, testType);
    }

    @PostMapping("/{id}/generate-test-points")
    public List<TestPoint> generateTestPoints(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String prompt = (body != null) ? body.get("prompt") : null;
        return requirementService.generateTestPoints(id, prompt);
    }

    @PostMapping("/{id}/confirm-test-points")
    public void confirmTestPoints(@PathVariable Long id) {
        requirementService.confirmTestPoints(id);
    }
}
