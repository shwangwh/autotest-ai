package com.testplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.testplatform.entity.ExecutionResult;
import com.testplatform.entity.ExecutionTask;
import com.testplatform.entity.TestCase;
import com.testplatform.repository.ExecutionResultRepository;
import com.testplatform.repository.ExecutionTaskRepository;
import com.testplatform.repository.TestCaseRepository;
import com.testplatform.util.TestMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class AllureReportService {
    private static final String ALLURE_RESULTS_ROOT = "allure-results";
    private static final String ALLURE_REPORT_ROOT = "allure-reports";
    private static final String ALLURE_REPORT_DIR = ALLURE_REPORT_ROOT + "/";

    private final ExecutionResultRepository executionResultRepository;
    private final ExecutionTaskRepository executionTaskRepository;
    private final TestCaseRepository testCaseRepository;
    private final ObjectMapper objectMapper;
    private final String configuredAllureCommand;

    public AllureReportService(
        ExecutionResultRepository executionResultRepository,
        ExecutionTaskRepository executionTaskRepository,
        TestCaseRepository testCaseRepository,
        @Value("${allure.command:}") String configuredAllureCommand
    ) {
        this.executionResultRepository = executionResultRepository;
        this.executionTaskRepository = executionTaskRepository;
        this.testCaseRepository = testCaseRepository;
        this.configuredAllureCommand = configuredAllureCommand;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        // 创建报告目录
        ensureDirectory(Paths.get(ALLURE_RESULTS_ROOT));
        ensureDirectory(Paths.get(ALLURE_REPORT_ROOT));
    }

    public String generateReport(Long taskId) {
        ExecutionResult result = executionResultRepository.findByTaskId(taskId);
        if (result == null) {
            throw new IllegalStateException("Execution result not found for task " + taskId);
        }

        ExecutionTask task = executionTaskRepository.findById(taskId).orElse(null);
        TestCase testCase = task == null
            ? null
            : testCaseRepository.findById(task.getTestCaseId()).orElse(null);

        Path resultsPath = Paths.get(ALLURE_RESULTS_ROOT, String.valueOf(taskId));
        Path reportPath = Paths.get(ALLURE_REPORT_ROOT, String.valueOf(taskId));

        recreateDirectory(resultsPath);
        recreateDirectory(reportPath);

        try {
            writeReportArtifacts(taskId, result, task, testCase, resultsPath);
            runAllureGenerate(resultsPath, reportPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate Allure report: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Allure report generation was interrupted.", exception);
        }

        return "/allure-reports/" + taskId + "/index.html";
    }

    private void writeReportArtifacts(
        Long taskId,
        ExecutionResult result,
        ExecutionTask task,
        TestCase testCase,
        Path resultsPath
    ) throws IOException {
        String uuid = UUID.randomUUID().toString();
        String status = mapStatus(result.getResult());
        long start = toEpochMillis(resolveExecutedAt(result, task));
        long stop = Math.max(start + 1, System.currentTimeMillis());

        Map<String, Object> testResult = new LinkedHashMap<>();
        testResult.put("uuid", uuid);
        testResult.put("historyId", "task-" + taskId);
        testResult.put("name", resolveCaseTitle(taskId, testCase));
        testResult.put("fullName", "automation.task." + taskId);
        testResult.put("status", status);
        testResult.put("stage", "finished");
        testResult.put("description", buildDescription(taskId, task, result));
        testResult.put("labels", buildLabels(taskId, task));
        testResult.put("parameters", buildParameters(taskId, task, result));
        testResult.put("steps", buildSteps(resultsPath, result, status, start, stop));
        testResult.put("attachments", buildTopLevelAttachments(resultsPath, result));
        testResult.put("start", start);
        testResult.put("stop", stop);

        if ("failed".equals(status) || "broken".equals(status)) {
            Map<String, Object> statusDetails = new LinkedHashMap<>();
            statusDetails.put("message", defaultText(result.getErrorMessage(), "Assertion failed."));
            statusDetails.put("trace", buildFailureTrace(result));
            testResult.put("statusDetails", statusDetails);
        }

        writeJson(resultsPath.resolve(uuid + "-result.json"), testResult);
        writeExecutor(resultsPath, taskId);
        writeEnvironment(resultsPath, taskId, task, result);
    }

    private List<Map<String, Object>> buildLabels(Long taskId, ExecutionTask task) {
        List<Map<String, Object>> labels = new ArrayList<>();
        boolean webTask = isWebTask(task);
        labels.add(label("framework", "spring-boot"));
        labels.add(label("language", "java"));
        labels.add(label("suite", webTask ? "Web Automation" : "API Automation"));
        labels.add(label("subSuite", "Execution Task " + taskId));
        labels.add(label("host", "localhost"));
        labels.add(label("thread", "task-" + taskId));
        labels.add(label("feature", webTask ? "Web Automation" : "API Automation"));
        if (task != null && hasText(task.getEnvironment())) {
            labels.add(label("tag", task.getEnvironment()));
        }
        return labels;
    }

    private List<Map<String, Object>> buildParameters(Long taskId, ExecutionTask task, ExecutionResult result) {
        List<Map<String, Object>> parameters = new ArrayList<>();
        parameters.add(parameter("taskId", String.valueOf(taskId)));
        parameters.add(parameter("environment", task == null ? "N/A" : defaultText(task.getEnvironment(), "N/A")));
        parameters.add(parameter("requestMethod", defaultText(result.getRequestMethod(), "N/A")));
        parameters.add(parameter("requestUrl", defaultText(result.getRequestUrl(), "N/A")));
        parameters.add(parameter("responseStatus", String.valueOf(result.getResponseStatus())));
        return parameters;
    }

    private List<Map<String, Object>> buildSteps(Path resultsPath, ExecutionResult result, String finalStatus, long start, long stop)
        throws IOException {
        boolean webExecution = isWebExecution(result);
        JsonNode artifacts = parseArtifacts(result);
        if (webExecution && artifacts != null && artifacts.path("steps").isArray()) {
            return buildWebArtifactSteps(resultsPath, artifacts, finalStatus, start, stop);
        }
        long totalDuration = Math.max(4, stop - start);
        long stepDuration = Math.max(1, totalDuration / 4);
        long prepareStop = Math.min(stop - 3, start + stepDuration);
        long executeStop = Math.min(stop - 2, prepareStop + stepDuration);
        long assertStop = Math.min(stop - 1, executeStop + stepDuration);

        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step(
            webExecution ? "Prepare web run" : "Prepare request",
            "passed",
            start,
            prepareStop,
            List.of(
                writeJsonAttachment(resultsPath, "Request Headers", result.getRequestHeaders()),
                writeJsonAttachment(resultsPath, "Request Body", result.getRequestBody())
            ),
            List.of(
                parameter("method", defaultText(result.getRequestMethod(), "N/A")),
                parameter("url", defaultText(result.getRequestUrl(), "N/A"))
            )
        ));
        steps.add(step(
            webExecution ? "Dispatch web steps" : "Execute request",
            result.getResponseStatus() > 0 ? "passed" : "broken",
            prepareStop,
            executeStop,
            List.of(writeJsonAttachment(resultsPath, "Response Body", result.getResponseBody())),
            List.of(parameter("httpStatus", String.valueOf(result.getResponseStatus())))
        ));
        steps.add(step(
            webExecution ? "Validate page assertions" : "Validate assertions",
            finalStatus,
            executeStop,
            assertStop,
            List.of(writeTextAttachment(resultsPath, "Assertion Result", result.getAssertionResult(), "text/plain", ".txt")),
            List.of(parameter("result", defaultText(result.getResult(), "UNKNOWN")))
        ));
        steps.add(step(
            "Analyze execution",
            hasText(result.getLlmAnalysis()) ? "passed" : "skipped",
            assertStop,
            stop,
            List.of(writeTextAttachment(resultsPath, "LLM Analysis", result.getLlmAnalysis(), "text/plain", ".txt")),
            List.of()
        ));
        return steps;
    }

    private List<Map<String, Object>> buildTopLevelAttachments(Path resultsPath, ExecutionResult result) throws IOException {
        List<Map<String, Object>> attachments = new ArrayList<>();
        if (hasText(result.getErrorMessage())) {
            attachments.add(writeTextAttachment(resultsPath, "Error Message", result.getErrorMessage(), "text/plain", ".txt"));
        }
        JsonNode artifacts = parseArtifacts(result);
        if (artifacts != null) {
            attachments.add(writeJsonAttachment(resultsPath, "Web Execution Artifacts", result.getArtifacts()));
            JsonNode pageState = artifacts.path("pageState");
            if (pageState.isObject()) {
                attachments.add(writeJsonAttachment(resultsPath, "Final Page State", pageState.toString()));
                addScreenshotAttachment(resultsPath, attachments, "Final Screenshot", pageState.path("finalScreenshot").asText(null));
                addScreenshotAttachment(resultsPath, attachments, "Failure Screenshot", pageState.path("failureScreenshot").asText(null));
                if (hasText(pageState.path("dom").asText(null))) {
                    attachments.add(writeTextAttachment(resultsPath, "Page DOM Snapshot", pageState.path("dom").asText(), "text/html", ".html"));
                }
            }
            if (hasText(artifacts.path("failureStep").asText(null))) {
                attachments.add(writeTextAttachment(resultsPath, "Failure Step", artifacts.path("failureStep").asText(), "text/plain", ".txt"));
            }
        }
        return attachments;
    }

    private List<Map<String, Object>> buildWebArtifactSteps(
        Path resultsPath,
        JsonNode artifacts,
        String finalStatus,
        long start,
        long stop
    ) throws IOException {
        List<Map<String, Object>> steps = new ArrayList<>();
        JsonNode artifactSteps = artifacts.path("steps");
        int stepCount = Math.max(1, artifactSteps.size());
        long totalDuration = Math.max(stepCount, stop - start);
        long stepDuration = Math.max(1, totalDuration / stepCount);
        long currentStart = start;

        for (int i = 0; i < artifactSteps.size(); i++) {
            JsonNode artifactStep = artifactSteps.get(i);
            long currentStop = i == artifactSteps.size() - 1
                ? stop
                : Math.min(stop - 1, currentStart + stepDuration);
            String status = "FAILED".equalsIgnoreCase(artifactStep.path("status").asText())
                ? "failed"
                : "passed";
            List<Map<String, Object>> attachments = new ArrayList<>();
            attachments.add(writeJsonAttachment(resultsPath, "Step Detail", artifactStep.toString()));
            addScreenshotAttachment(resultsPath, attachments, "Step Screenshot", artifactStep.path("screenshot").asText(null));
            JsonNode pageState = artifactStep.path("pageState");
            if (pageState.isObject()) {
                attachments.add(writeJsonAttachment(resultsPath, "Failure Page State", pageState.toString()));
                if (hasText(pageState.path("dom").asText(null))) {
                    attachments.add(writeTextAttachment(resultsPath, "Failure DOM Snapshot", pageState.path("dom").asText(), "text/html", ".html"));
                }
            }
            if (hasText(artifactStep.path("error").asText(null))) {
                attachments.add(writeTextAttachment(resultsPath, "Step Error", artifactStep.path("error").asText(), "text/plain", ".txt"));
            }

            steps.add(step(
                defaultText(artifactStep.path("name").asText(null), "Web step " + (i + 1)),
                status,
                currentStart,
                currentStop,
                attachments,
                List.of(
                    parameter("action", defaultText(artifactStep.path("action").asText(null), "N/A")),
                    parameter("selector", defaultText(artifactStep.path("selector").asText(null), "N/A")),
                    parameter("url", defaultText(artifactStep.path("pageUrl").asText(null), "N/A"))
                )
            ));
            currentStart = currentStop;
        }

        if (steps.isEmpty()) {
            steps.add(step(
                "Dispatch web steps",
                finalStatus,
                start,
                stop,
                List.of(writeJsonAttachment(resultsPath, "Web Execution Artifacts", artifacts.toString())),
                List.of(parameter("status", defaultText(artifacts.path("status").asText(null), "UNKNOWN")))
            ));
        }
        return steps;
    }

    private JsonNode parseArtifacts(ExecutionResult result) {
        if (result == null || !hasText(result.getArtifacts())) {
            return null;
        }
        try {
            return objectMapper.readTree(result.getArtifacts());
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private void addScreenshotAttachment(Path resultsPath, List<Map<String, Object>> attachments, String name, String screenshotPath)
        throws IOException {
        if (!hasText(screenshotPath)) {
            return;
        }
        Path sourcePath = Paths.get(screenshotPath);
        if (!Files.isRegularFile(sourcePath)) {
            attachments.add(writeTextAttachment(resultsPath, name + " Missing", screenshotPath, "text/plain", ".txt"));
            return;
        }
        attachments.add(writeBinaryAttachment(resultsPath, name, sourcePath, "image/png", ".png"));
    }

    private void writeExecutor(Path resultsPath, Long taskId) throws IOException {
        Map<String, Object> executor = new LinkedHashMap<>();
        executor.put("name", "Smart Test Platform");
        executor.put("type", "local");
        executor.put("buildName", "Execution Task #" + taskId);
        executor.put("buildOrder", taskId);
        executor.put("reportName", "Allure Report");
        writeJson(resultsPath.resolve("executor.json"), executor);
    }

    private void writeEnvironment(Path resultsPath, Long taskId, ExecutionTask task, ExecutionResult result) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("Task ID", String.valueOf(taskId));
        properties.setProperty("Environment", task == null ? "N/A" : defaultText(task.getEnvironment(), "N/A"));
        properties.setProperty("Request URL", defaultText(result.getRequestUrl(), "N/A"));
        properties.setProperty("Request Method", defaultText(result.getRequestMethod(), "N/A"));
        properties.setProperty("Response Status", String.valueOf(result.getResponseStatus()));
        if (result.getExecutedAt() != null) {
            properties.setProperty("Executed At", result.getExecutedAt().toString());
        }

        try (var writer = Files.newBufferedWriter(resultsPath.resolve("environment.properties"), StandardCharsets.UTF_8)) {
            properties.store(writer, "Allure environment");
        }
    }

    private void runAllureGenerate(Path resultsPath, Path reportPath) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(resolveAllureCommand());
        command.add("generate");
        command.add(resultsPath.toAbsolutePath().toString());
        command.add("-o");
        command.add(reportPath.toAbsolutePath().toString());
        command.add("--clean");

        Process process = new ProcessBuilder(command)
            .directory(Paths.get(".").toAbsolutePath().normalize().toFile())
            .redirectErrorStream(true)
            .start();

        String output = readProcessOutput(process.getInputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException(output.isBlank() ? "Allure CLI exited with code " + exitCode : output.trim());
        }
    }

    private List<String> resolveAllureCommand() {
        if (hasText(configuredAllureCommand)) {
            return commandFor(configuredAllureCommand.trim());
        }

        String pathCommand = findAllureOnPath();
        if (hasText(pathCommand)) {
            return commandFor(pathCommand);
        }

        return commandFor("allure");
    }

    private String findAllureOnPath() {
        String pathValue = System.getenv("PATH");
        if (!hasText(pathValue)) {
            return null;
        }

        String[] executableNames = isWindows()
            ? new String[] { "allure.bat", "allure.cmd", "allure.exe", "allure" }
            : new String[] { "allure" };

        for (String directory : pathValue.split(java.io.File.pathSeparator)) {
            if (!hasText(directory)) {
                continue;
            }
            Path basePath = Paths.get(directory);
            for (String executableName : executableNames) {
                Path candidate = basePath.resolve(executableName);
                if (Files.isRegularFile(candidate)) {
                    return candidate.toString();
                }
            }
        }

        return null;
    }

    private List<String> commandFor(String command) {
        if (isWindows() && (command.endsWith(".bat") || command.endsWith(".cmd"))) {
            return List.of("cmd.exe", "/c", command);
        }
        return List.of(command);
    }

    private Map<String, Object> step(
        String name,
        String status,
        long start,
        long stop,
        List<Map<String, Object>> attachments,
        List<Map<String, Object>> parameters
    ) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("name", name);
        step.put("status", status);
        step.put("stage", "finished");
        step.put("attachments", attachments);
        step.put("parameters", parameters);
        step.put("start", start);
        step.put("stop", Math.max(stop, start + 1));
        return step;
    }

    private Map<String, Object> parameter(String name, String value) {
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("name", name);
        parameter.put("value", value);
        return parameter;
    }

    private Map<String, Object> label(String name, String value) {
        Map<String, Object> label = new LinkedHashMap<>();
        label.put("name", name);
        label.put("value", value);
        return label;
    }

    private Map<String, Object> writeJsonAttachment(Path resultsPath, String name, String content) throws IOException {
        return writeTextAttachment(resultsPath, name, prettyJsonOrRaw(content), "application/json", ".json");
    }

    private Map<String, Object> writeTextAttachment(
        Path resultsPath,
        String name,
        String content,
        String mimeType,
        String extension
    ) throws IOException {
        String source = UUID.randomUUID() + "-attachment" + extension;
        Files.writeString(resultsPath.resolve(source), defaultText(content, "(empty)"), StandardCharsets.UTF_8);

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("name", name);
        attachment.put("source", source);
        attachment.put("type", mimeType);
        return attachment;
    }

    private Map<String, Object> writeBinaryAttachment(
        Path resultsPath,
        String name,
        Path sourcePath,
        String mimeType,
        String extension
    ) throws IOException {
        String source = UUID.randomUUID() + "-attachment" + extension;
        Files.copy(sourcePath, resultsPath.resolve(source), StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("name", name);
        attachment.put("source", source);
        attachment.put("type", mimeType);
        return attachment;
    }

    private String buildDescription(Long taskId, ExecutionTask task, ExecutionResult result) {
        boolean webTask = isWebTask(task);
        return """
            Smart Test Platform generated this %s report for execution task #%d.

            Environment: %s
            Entry: %s %s
            Final status: %s
            """.formatted(
            webTask ? "Web automation" : "API automation",
            taskId,
            task == null ? "N/A" : defaultText(task.getEnvironment(), "N/A"),
            defaultText(result.getRequestMethod(), "N/A"),
            defaultText(result.getRequestUrl(), "N/A"),
            defaultText(result.getResult(), "UNKNOWN")
        ).trim();
    }

    private String buildFailureTrace(ExecutionResult result) {
        return """
            Request: %s %s
            Response status: %d
            Assertion: %s
            Error: %s
            """.formatted(
            defaultText(result.getRequestMethod(), "N/A"),
            defaultText(result.getRequestUrl(), "N/A"),
            result.getResponseStatus(),
            defaultText(result.getAssertionResult(), "N/A"),
            defaultText(result.getErrorMessage(), "Unknown failure")
        ).trim();
    }

    private boolean isWebTask(ExecutionTask task) {
        return task != null && "WEB".equalsIgnoreCase(task.getRunnerType());
    }

    private boolean isWebExecution(ExecutionResult result) {
        return result != null && "WEB".equalsIgnoreCase(result.getRequestMethod());
    }

    private String resolveCaseTitle(Long taskId, TestCase testCase) {
        if (testCase != null && hasText(testCase.getTitle())) {
            return testCase.getTitle();
        }
        return "Execution Task #" + taskId;
    }

    private LocalDateTime resolveExecutedAt(ExecutionResult result, ExecutionTask task) {
        if (result.getExecutedAt() != null) {
            return result.getExecutedAt();
        }
        if (task != null && task.getExecutedAt() != null) {
            return task.getExecutedAt();
        }
        return LocalDateTime.now();
    }

    private long toEpochMillis(LocalDateTime localDateTime) {
        return localDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }

    private String mapStatus(String result) {
        if (!hasText(result)) {
            return "unknown";
        }
        return switch (result.toUpperCase(Locale.ROOT)) {
            case "PASSED" -> "passed";
            case "FAILED" -> "failed";
            default -> "unknown";
        };
    }

    private String prettyJsonOrRaw(String value) {
        if (!hasText(value)) {
            return "";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(value));
        } catch (JsonProcessingException exception) {
            return value;
        }
    }

    private void writeJson(Path path, Object data) throws IOException {
        objectMapper.writeValue(path.toFile(), data);
    }

    private void recreateDirectory(Path directory) {
        try {
            if (Files.exists(directory)) {
                try (Stream<Path> walk = Files.walk(directory)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
                }
            }
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare directory " + directory, exception);
        }
    }

    private void ensureDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create directory " + directory, exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete " + path, exception);
        }
    }

    private String readProcessOutput(InputStream inputStream) throws IOException {
        try (inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            inputStream.transferTo(outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    // Legacy custom HTML report generator kept temporarily for reference only.
    public String generateReportLegacy(Long taskId) {
        String reportDir = ALLURE_REPORT_DIR + taskId;
        Path reportPath = Paths.get(reportDir);
        try {
            if (!Files.exists(reportPath)) {
                Files.createDirectories(reportPath);
            }

            ExecutionResult result = executionResultRepository.findByTaskId(taskId);
            ExecutionTask task = executionTaskRepository.findById(taskId).orElse(null);

            String testCaseTitle = "未知用例";
            if (task != null) {
                TestCase testCase = testCaseRepository.findById(task.getTestCaseId()).orElse(null);
                if (testCase != null) {
                    testCaseTitle = testCase.getTitle();
                }
            }

            String html = buildReportHtml(taskId, result, task, testCaseTitle);
            Files.writeString(reportPath.resolve("index.html"), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reportDir;
    }

    private String buildReportHtml(Long taskId, ExecutionResult result, ExecutionTask task, String testCaseTitle) {
        boolean passed = result != null && "PASSED".equals(result.getResult());
        String statusText = passed ? "PASSED" : "FAILED";
        String statusColor = passed ? "#00c261" : "#ed0303";
        String statusBg = passed ? "#f2fcf7" : "#fef2f2";
        String statusBorder = passed ? "#b2edd0" : "#fab3b3";

        String environment = task != null ? safe(task.getEnvironment()) : "N/A";
        String executedAt = result != null && result.getExecutedAt() != null
            ? result.getExecutedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String requestUrl = result != null ? safe(result.getRequestUrl()) : "-";
        String requestMethod = result != null ? safe(result.getRequestMethod()) : "-";
        String requestHeaders = result != null ? formatJson(safe(result.getRequestHeaders())) : "-";
        String requestBody = result != null ? formatJson(safe(result.getRequestBody())) : "-";
        int responseStatus = result != null ? result.getResponseStatus() : 0;
        String responseBody = result != null ? formatJson(safe(result.getResponseBody())) : "-";
        String assertionResult = result != null ? safe(result.getAssertionResult()) : "-";
        String llmAnalysis = result != null ? safe(result.getLlmAnalysis()) : "-";
        String errorMessage = result != null && result.getErrorMessage() != null ? safe(result.getErrorMessage()) : "";

        int responseStatusColor = responseStatus >= 200 && responseStatus < 300 ? 1 : 0; // 1=green, 0=red

        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>测试报告 - 任务 #%d</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
      background: #f5f7fb;
      color: #323233;
      line-height: 1.6;
      min-height: 100vh;
    }
    /* Header */
    .report-header {
      background: linear-gradient(135deg, #783887 0%%, #9441b1 50%%, #b379c8 100%%);
      color: white;
      padding: 32px 40px;
    }
    .report-header h1 {
      font-size: 24px;
      font-weight: 600;
      margin-bottom: 4px;
    }
    .report-header .subtitle {
      font-size: 14px;
      opacity: 0.85;
    }
    .header-meta {
      display: flex;
      gap: 24px;
      margin-top: 16px;
      flex-wrap: wrap;
    }
    .header-meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      opacity: 0.9;
    }
    .header-meta-item .label { opacity: 0.7; }
    /* Container */
    .container {
      max-width: 1100px;
      margin: 0 auto;
      padding: 24px 20px 48px;
    }
    /* Status Banner */
    .status-banner {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px 24px;
      border-radius: 12px;
      margin-bottom: 24px;
      border: 1px solid %s;
      background: %s;
    }
    .status-icon {
      width: 48px;
      height: 48px;
      border-radius: 50%%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      font-weight: 700;
      color: white;
      background: %s;
      flex-shrink: 0;
    }
    .status-text h2 {
      font-size: 20px;
      font-weight: 600;
      color: %s;
    }
    .status-text p {
      font-size: 14px;
      color: #646466;
      margin-top: 2px;
    }
    /* Cards */
    .card {
      background: white;
      border-radius: 12px;
      margin-bottom: 20px;
      box-shadow: 0 1px 4px rgba(0,0,0,0.06);
      overflow: hidden;
    }
    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px 24px;
      font-size: 16px;
      font-weight: 600;
      border-bottom: 1px solid #ededf1;
      background: #fafbfc;
    }
    .card-title .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%%;
      background: #783887;
    }
    .card-body {
      padding: 20px 24px;
    }
    /* Info Grid */
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 16px;
    }
    .info-item {
      padding: 12px 16px;
      background: #f9f9fe;
      border-radius: 8px;
      border-left: 3px solid #783887;
    }
    .info-item .label {
      font-size: 12px;
      color: #7d7d7f;
      margin-bottom: 4px;
    }
    .info-item .value {
      font-size: 14px;
      font-weight: 500;
      color: #323233;
      word-break: break-all;
    }
    /* Code Block */
    .code-block {
      background: #1d2129;
      color: #e8e8e8;
      border-radius: 8px;
      padding: 16px 20px;
      font-family: 'Consolas', 'Monaco', 'Menlo', monospace;
      font-size: 13px;
      line-height: 1.7;
      overflow-x: auto;
      white-space: pre-wrap;
      word-break: break-all;
      margin-top: 12px;
    }
    .code-block .key { color: #94bfff; }
    .code-block .string { color: #7be188; }
    .code-block .number { color: #fadc6d; }
    .code-block .bool { color: #f98981; }
    /* Section label */
    .section-label {
      font-size: 13px;
      font-weight: 600;
      color: #646466;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 8px;
      margin-top: 20px;
    }
    .section-label:first-child { margin-top: 0; }
    /* Response Status Badge */
    .http-status {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      border-radius: 16px;
      font-size: 14px;
      font-weight: 600;
    }
    .http-status.ok {
      background: #f2fcf7;
      color: #00a552;
      border: 1px solid #b2edd0;
    }
    .http-status.err {
      background: #fef2f2;
      color: #ed0303;
      border: 1px solid #fab3b3;
    }
    /* Method badge */
    .method-badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 700;
      background: #783887;
      color: white;
      letter-spacing: 0.5px;
    }
    /* Analysis box */
    .analysis-box {
      background: #f2e9f6;
      border-radius: 10px;
      padding: 16px 20px;
      margin-top: 12px;
      border-left: 4px solid #783887;
      font-size: 14px;
      line-height: 1.8;
      color: #323233;
      white-space: pre-wrap;
    }
    /* Error box */
    .error-box {
      background: #fef2f2;
      border-radius: 10px;
      padding: 16px 20px;
      margin-top: 12px;
      border-left: 4px solid #ed0303;
      font-size: 14px;
      color: #c90303;
    }
    /* Assertion */
    .assertion-text {
      padding: 12px 16px;
      background: #f9f9fe;
      border-radius: 8px;
      font-size: 14px;
      margin-top: 12px;
    }
    /* Footer */
    .report-footer {
      text-align: center;
      padding: 24px;
      color: #959598;
      font-size: 12px;
    }
    /* Timeline */
    .timeline-step {
      display: flex;
      gap: 16px;
      padding: 12px 0;
      border-bottom: 1px solid #f2f3f5;
    }
    .timeline-step:last-child { border-bottom: none; }
    .timeline-num {
      width: 28px;
      height: 28px;
      border-radius: 50%%;
      background: #783887;
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 600;
      flex-shrink: 0;
    }
    .timeline-num.done { background: #00c261; }
    .timeline-num.fail { background: #ed0303; }
    .timeline-content { flex: 1; }
    .timeline-content .step-title {
      font-size: 14px;
      font-weight: 500;
    }
    .timeline-content .step-detail {
      font-size: 13px;
      color: #646466;
      margin-top: 2px;
    }
  </style>
</head>
<body>
  <div class="report-header">
    <h1>🧪 测试执行报告</h1>
    <p class="subtitle">%s</p>
    <div class="header-meta">
      <div class="header-meta-item">
        <span class="label">任务ID:</span>
        <span>#%d</span>
      </div>
      <div class="header-meta-item">
        <span class="label">环境:</span>
        <span>%s</span>
      </div>
      <div class="header-meta-item">
        <span class="label">执行时间:</span>
        <span>%s</span>
      </div>
    </div>
  </div>

  <div class="container">
    <!-- 状态概览 -->
    <div class="status-banner">
      <div class="status-icon">%s</div>
      <div class="status-text">
        <h2>%s</h2>
        <p>%s</p>
      </div>
    </div>

    <!-- 执行步骤 -->
    <div class="card">
      <div class="card-title"><span class="dot"></span> 执行步骤</div>
      <div class="card-body">
        <div class="timeline-step">
          <div class="timeline-num done">1</div>
          <div class="timeline-content">
            <div class="step-title">构造请求</div>
            <div class="step-detail">%s  %s</div>
          </div>
        </div>
        <div class="timeline-step">
          <div class="timeline-num done">2</div>
          <div class="timeline-content">
            <div class="step-title">发送请求</div>
            <div class="step-detail">Headers: %s  |  Body: %s</div>
          </div>
        </div>
        <div class="timeline-step">
          <div class="timeline-num %s">3</div>
          <div class="timeline-content">
            <div class="step-title">接收响应</div>
            <div class="step-detail">HTTP %d</div>
          </div>
        </div>
        <div class="timeline-step">
          <div class="timeline-num %s">4</div>
          <div class="timeline-content">
            <div class="step-title">断言校验</div>
            <div class="step-detail">%s → %s</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 请求详情 -->
    <div class="card">
      <div class="card-title"><span class="dot"></span> 请求详情</div>
      <div class="card-body">
        <div class="info-grid">
          <div class="info-item">
            <div class="label">请求方法</div>
            <div class="value"><span class="method-badge">%s</span></div>
          </div>
          <div class="info-item">
            <div class="label">请求URL</div>
            <div class="value">%s</div>
          </div>
        </div>

        <div class="section-label">请求头 (Request Headers)</div>
        <div class="code-block">%s</div>

        <div class="section-label">请求体 (Request Body)</div>
        <div class="code-block">%s</div>
      </div>
    </div>

    <!-- 响应详情 -->
    <div class="card">
      <div class="card-title"><span class="dot"></span> 响应详情</div>
      <div class="card-body">
        <div class="section-label">响应状态</div>
        <span class="http-status %s">● HTTP %d</span>

        <div class="section-label">响应体 (Response Body)</div>
        <div class="code-block">%s</div>
      </div>
    </div>

    <!-- 断言结果 -->
    <div class="card">
      <div class="card-title"><span class="dot"></span> 断言校验</div>
      <div class="card-body">
        <div class="assertion-text">%s</div>
        %s
      </div>
    </div>

    <!-- AI 分析 -->
    <div class="card">
      <div class="card-title"><span class="dot"></span> 🤖 AI 智能分析</div>
      <div class="card-body">
        <div class="analysis-box">%s</div>
      </div>
    </div>
  </div>

  <div class="report-footer">
    智能测试平台 · Allure Report · Generated at %s
  </div>
</body>
</html>
""".formatted(
            // title
            taskId,
            // header subtitle, meta
            statusBorder, statusBg, statusColor, statusColor,
            testCaseTitle,
            taskId, environment, executedAt,
            // status banner
            passed ? "✓" : "✗",
            statusText,
            passed ? "执行结果正常，符合预期，响应状态码为 " + responseStatus + "，返回数据结构正确。"
                   : "执行结果异常，" + (errorMessage.isEmpty() ? "响应不符合预期。" : errorMessage),
            // timeline
            requestMethod, requestUrl,
            requestHeaders, requestBody,
            responseStatusColor == 1 ? "done" : "fail",
            responseStatus,
            passed ? "done" : "fail",
            assertionResult, statusText,
            // request card
            requestMethod, requestUrl,
            requestHeaders,
            requestBody,
            // response card
            responseStatusColor == 1 ? "ok" : "err",
            responseStatus,
            responseBody,
            // assertion card
            assertionResult,
            errorMessage.isEmpty() ? "" : "<div class=\"error-box\">错误信息: " + errorMessage + "</div>",
            // AI analysis
            llmAnalysis,
            // footer
            executedAt
        );
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String formatJson(String json) {
        if (json == null || json.isEmpty()) return "-";
        // 简单格式化: 在 , 和 : 后面换行/加空格
        try {
            return json
                .replace(",\"", ",\n\"")
                .replace("{\"", "{\n  \"")
                .replace("}", "\n}")
                .replace("\":", "\": ");
        } catch (Exception e) {
            return json;
        }
    }
}
