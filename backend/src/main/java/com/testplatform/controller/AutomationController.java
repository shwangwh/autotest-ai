package com.testplatform.controller;

import com.testplatform.entity.ExecutionTask;
import com.testplatform.entity.ExecutionResult;
import com.testplatform.service.AllureReportService;
import com.testplatform.service.AutomationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/automation")
public class AutomationController {
    @Autowired
    private AutomationService automationService;

    @Autowired
    private AllureReportService allureReportService;

    @GetMapping("/tasks")
    public List<ExecutionTask> getAllTasks() {
        return automationService.getAllTasks();
    }

    @GetMapping("/tasks/project/{projectId}")
    public List<ExecutionTask> getTasksByProjectId(@PathVariable Long projectId) {
        return automationService.getTasksByProjectId(projectId);
    }

    @PostMapping("/tasks")
    public ExecutionTask createTask(@RequestBody Map<String, Object> params) {
        Long projectId = Long.valueOf(params.get("projectId").toString());
        Long testCaseId = Long.valueOf(params.get("testCaseId").toString());
        String environment = params.get("environment").toString();
        return automationService.createTask(projectId, testCaseId, environment);
    }

    @PostMapping("/tasks/batch")
    public List<ExecutionTask> createBatchTasks(@RequestBody Map<String, Object> params) {
        Long projectId = Long.valueOf(params.get("projectId").toString());
        @SuppressWarnings("unchecked")
        List<Long> testCaseIds = (List<Long>) params.get("testCaseIds");
        String environment = params.get("environment").toString();
        return automationService.createBatchTasks(projectId, testCaseIds, environment);
    }

    @PostMapping("/tasks/{id}/execute")
    public ExecutionResult executeTask(@PathVariable Long id) {
        return automationService.executeTask(id);
    }

    @PostMapping("/tasks/batch/execute")
    public List<ExecutionResult> executeBatchTasks(@RequestBody List<Long> taskIds) {
        return automationService.executeBatchTasks(taskIds);
    }

    @GetMapping("/results/{taskId}")
    public ExecutionResult getExecutionResult(@PathVariable Long taskId) {
        return automationService.getExecutionResult(taskId);
    }

    @PostMapping("/tasks/{id}/retry")
    public ExecutionTask retryTask(@PathVariable Long id) {
        return automationService.retryTask(id);
    }

    @PostMapping("/tasks/{id}/allure-report")
    public ResponseEntity<Map<String, String>> generateAllureReport(@PathVariable Long id) {
        try {
            String reportUrl = allureReportService.generateReport(id);
            Map<String, String> response = new HashMap<>();
            response.put("reportUrl", reportUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
