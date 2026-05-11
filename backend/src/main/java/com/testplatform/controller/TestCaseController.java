package com.testplatform.controller;

import com.testplatform.entity.TestCase;
import com.testplatform.service.TestCaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-cases")
public class TestCaseController {
    @Autowired
    private TestCaseService testCaseService;

    @GetMapping
    public List<TestCase> getAllTestCases() {
        return testCaseService.getAllTestCases();
    }

    @GetMapping("/project/{projectId}")
    public List<TestCase> getTestCasesByProjectId(@PathVariable Long projectId) {
        return testCaseService.getTestCasesByProjectId(projectId);
    }

    @GetMapping("/test-point/{testPointId}")
    public List<TestCase> getTestCasesByTestPointId(@PathVariable Long testPointId) {
        return testCaseService.getTestCasesByTestPointId(testPointId);
    }

    @GetMapping("/{id}")
    public TestCase getTestCaseById(@PathVariable Long id) {
        return testCaseService.getTestCaseById(id);
    }

    @PostMapping("/generate")
    public List<TestCase> generateTestCases(
            @RequestParam Long projectId,
            @RequestParam Long testPointId,
            @RequestParam(required = false) String prompt
    ) {
        return testCaseService.generateTestCases(projectId, testPointId, prompt);
    }

    @PostMapping("/generate/batch")
    public List<TestCase> generateBatchTestCases(
            @RequestParam Long projectId,
            @RequestParam List<Long> testPointIds,
            @RequestParam(required = false) String prompt
    ) {
        return testCaseService.generateBatchTestCases(projectId, testPointIds, prompt);
    }

    @PutMapping("/{id}")
    public TestCase updateTestCase(@PathVariable Long id, @RequestBody TestCase testCase) {
        return testCaseService.updateTestCase(id, testCase);
    }

    @DeleteMapping("/{id}")
    public void deleteTestCase(@PathVariable Long id) {
        testCaseService.deleteTestCase(id);
    }

    @PostMapping
    public TestCase addTestCase(@RequestBody TestCase testCase) {
        return testCaseService.addTestCase(testCase);
    }
}
