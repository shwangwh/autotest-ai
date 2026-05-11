package com.testplatform.controller;

import com.testplatform.entity.TestPoint;
import com.testplatform.service.TestPointService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test-points")
public class TestPointController {
    private final TestPointService testPointService;

    public TestPointController(TestPointService testPointService) {
        this.testPointService = testPointService;
    }

    @GetMapping("/requirement/{requirementId}")
    public List<TestPoint> getByRequirementId(@PathVariable Long requirementId) {
        return testPointService.getByRequirementId(requirementId);
    }

    @GetMapping
    public List<TestPoint> getAllTestPoints() {
        return testPointService.getAllTestPoints();
    }

    @GetMapping("/project/{projectId}")
    public List<TestPoint> getByProjectId(@PathVariable Long projectId) {
        return testPointService.getByProjectId(projectId);
    }

    @PostMapping
    public TestPoint createTestPoint(@RequestBody TestPoint testPoint) {
        return testPointService.createTestPoint(testPoint);
    }

    @PutMapping("/{id}")
    public TestPoint updateTestPoint(@PathVariable Long id, @RequestBody TestPoint testPoint) {
        return testPointService.updateTestPoint(id, testPoint);
    }

    @DeleteMapping("/{id}")
    public void deleteTestPoint(@PathVariable Long id) {
        testPointService.deleteTestPoint(id);
    }
}
