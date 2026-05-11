package com.testplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.testplatform.entity.ExecutionResult;
import com.testplatform.entity.ExecutionTask;
import com.testplatform.entity.TestCase;
import com.testplatform.repository.ExecutionResultRepository;
import com.testplatform.repository.ExecutionTaskRepository;
import com.testplatform.repository.TestCaseRepository;
import com.testplatform.util.LLMLogger;
import com.testplatform.util.TestMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AutomationService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path WEB_ARTIFACTS_ROOT = Path.of("automation-artifacts");
    private final ExecutionTaskRepository executionTaskRepository;
    private final ExecutionResultRepository executionResultRepository;
    private final TestCaseRepository testCaseRepository;
    private final AllureReportService allureReportService;
    private final LLMService llmService;
    private final WebAutomationExecutor webAutomationExecutor;
    private final int serverPort;
    private final String defaultWebBaseUrl;
    private final String productionWebBaseUrl;
    private final String stagingWebBaseUrl;
    private final String devWebBaseUrl;

    // 私有辅助方法，在构造函数之前定义
    private String normalizeBaseUrl(String baseUrl) {
        String fallback = "http://127.0.0.1:" + serverPort;
        String normalized = (baseUrl == null || baseUrl.isBlank()) ? fallback : baseUrl.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private boolean useRealWebExecutor() {
        return true;
    }

    private ExecutionResult executeRealWebTask(ExecutionTask task, ExecutionResult result, TestCase testCase) {
        return webAutomationExecutor.execute(task, result, testCase);
    }

    public AutomationService(
        ExecutionTaskRepository executionTaskRepository,
        ExecutionResultRepository executionResultRepository,
        TestCaseRepository testCaseRepository,
        AllureReportService allureReportService,
        LLMService llmService,
        WebAutomationExecutor webAutomationExecutor,
        @Value("${server.port:8080}") int serverPort,
        @Value("${web.automation.base-url.default:http://127.0.0.1:${server.port:8080}}") String defaultWebBaseUrl,
        @Value("${web.automation.base-url.production:http://127.0.0.1:${server.port:8080}}") String productionWebBaseUrl,
        @Value("${web.automation.base-url.staging:http://127.0.0.1:${server.port:8080}}") String stagingWebBaseUrl,
        @Value("${web.automation.base-url.dev:http://127.0.0.1:${server.port:8080}}") String devWebBaseUrl
    ) {
        this.executionTaskRepository = executionTaskRepository;
        this.executionResultRepository = executionResultRepository;
        this.testCaseRepository = testCaseRepository;
        this.allureReportService = allureReportService;
        this.llmService = llmService;
        this.webAutomationExecutor = webAutomationExecutor;
        this.serverPort = serverPort;
        this.defaultWebBaseUrl = normalizeBaseUrl(defaultWebBaseUrl);
        this.productionWebBaseUrl = normalizeBaseUrl(productionWebBaseUrl);
        this.stagingWebBaseUrl = normalizeBaseUrl(stagingWebBaseUrl);
        this.devWebBaseUrl = normalizeBaseUrl(devWebBaseUrl);
    }

    public List<ExecutionTask> getAllTasks() {
        List<ExecutionTask> tasks = executionTaskRepository.findAll();
        tasks.forEach(this::normalizeTask);
        return tasks;
    }

    public List<ExecutionTask> getTasksByProjectId(Long projectId) {
        List<ExecutionTask> tasks = executionTaskRepository.findByProjectId(projectId);
        tasks.forEach(this::normalizeTask);
        return tasks;
    }

    public ExecutionTask createTask(Long projectId, Long testCaseId, String environment) {
        TestCase testCase = testCaseRepository.findById(testCaseId).orElse(null);
        if (testCase == null) {
            return null;
        }

        boolean interfaceCase = TestMetadata.isInterfaceTest(testCase.getTestType(), testCase.getExecutionType(), testCase.getRequestData());
        boolean webCase = TestMetadata.isWebTest(testCase.getTestType(), testCase.getExecutionType());
        if (!interfaceCase && !webCase) {
            return null;
        }

        ExecutionTask task = new ExecutionTask();
        task.setProjectId(projectId);
        task.setTestCaseId(testCaseId);
        task.setPlanId(1L);
        task.setEnvironment(environment);
        task.setTaskType(testCase.getTestType());
        task.setRunnerType(webCase ? "WEB" : "LOCAL");
        return executionTaskRepository.save(task);
    }

    public ExecutionResult executeTask(Long taskId) {
        ExecutionTask task = executionTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return null;
        }

        task.setStatus("RUNNING");
        executionTaskRepository.save(task);

        ExecutionResult result = executionResultRepository.findByTaskId(taskId);
        if (result == null) {
            result = new ExecutionResult();
            result.setTaskId(taskId);
        }

        // 获取测试用例，提取步骤和预期结果
        TestCase testCase = testCaseRepository.findById(task.getTestCaseId()).orElse(null);
        if (testCase == null) {
            task.setStatus("FAILED");
            executionTaskRepository.save(task);
            result.setTestType(task.getTaskType());
            result.setResult("FAILED");
            result.setErrorMessage("Test case not found for execution task.");
            return executionResultRepository.save(result);
        }
        if (TestMetadata.isWebTest(testCase.getTestType(), testCase.getExecutionType())) {
            return executeWebTask(task, result, testCase);
        }
        if (!TestMetadata.isInterfaceTest(testCase.getTestType(), testCase.getExecutionType(), testCase.getRequestData())) {
            task.setStatus("FAILED");
            executionTaskRepository.save(task);
            result.setTestType(task.getTaskType());
            result.setResult("FAILED");
            result.setErrorMessage("Unsupported test case type for current automation module.");
            return executionResultRepository.save(result);
        }
        result.setTestType(testCase.getTestType());
        String testSteps = testCase != null ? testCase.getSteps() : null;
        String expectedResultText = testCase != null ? testCase.getExpectedResult() : null;

        // 双保险：优先使用结构化的 requestData，正则解析作为回退
        ParsedRequest parsedReq = parseRequestData(testCase);
        if (parsedReq == null) {
            parsedReq = parseTestSteps(testSteps);
            if (parsedReq != null) {
                LLMLogger.info("AutomationService", "executeTask", "使用正则解析测试步骤获取请求参数");
            }
        } else {
            LLMLogger.info("AutomationService", "executeTask", "使用结构化 requestData 获取请求参数");
        }

        try {
            HttpResponse<String> response;
            String requestUrl, requestMethod, requestBody, requestHeaders;

            if (parsedReq != null && parsedReq.path != null) {
                requestUrl = "http://127.0.0.1:" + serverPort + parsedReq.path;
                requestMethod = parsedReq.method;
                requestBody = parsedReq.body;
                requestHeaders = "{\"Content-Type\":\"" + parsedReq.contentType + "\"}";
                LLMLogger.info("AutomationService", "executeTask",
                    "执行请求: " + requestMethod + " " + requestUrl + " body=" + requestBody);
                response = callEndpoint(requestMethod, requestUrl, parsedReq.contentType, requestBody);
            } else {
                requestUrl = "http://127.0.0.1:" + serverPort + "/api/auth/login";
                requestMethod = "POST";
                requestBody = "{\"username\":\"admin\",\"password\":\"1\"}";
                requestHeaders = "{\"Content-Type\":\"application/json\"}";
                LLMLogger.info("AutomationService", "executeTask",
                    "无法获取请求参数，使用默认登录接口");
                response = callLoginEndpoint();
            }

            result.setRequestUrl(requestUrl);
            result.setRequestMethod(requestMethod);
            result.setRequestHeaders(requestHeaders);
            result.setRequestBody(requestBody);
            result.setResponseStatus(response.statusCode());
            result.setResponseBody(response.body());

            // 基于预期结果逐条断言，生成详细断言报告
            StringBuilder assertionDetails = new StringBuilder();
            boolean passed = evaluateResult(response, expectedResultText, assertionDetails);
            String assertionText = assertionDetails.toString();
            // 限制assertionResult长度，避免数据库错误
            if (assertionText.length() > 65535) {
                assertionText = assertionText.substring(0, 65535);
            }
            result.setAssertionResult(assertionText);
            result.setResult(passed ? "PASSED" : "FAILED");
            result.setErrorMessage(passed ? null : "实际响应与预期结果不符，详见断言报告");

            // 调用 LLM 做深度分析，传入测试步骤和预期结果
            LLMLogger.info("AutomationService", "executeTask", "开始调用LLM服务分析执行结果");
            try {
                String llmResult = llmService.analyzeExecutionResult(
                    buildExecutionSummary(requestUrl, requestMethod, requestBody, response),
                    testSteps, expectedResultText);
                // 限制llmAnalysis长度，避免数据库错误
                if (llmResult.length() > 65535) {
                    llmResult = llmResult.substring(0, 65535);
                }
                result.setLlmAnalysis(llmResult);
                LLMLogger.info("AutomationService", "executeTask", "LLM服务分析执行结果完成");
            } catch (Exception e) {
                LLMLogger.error("AutomationService", "executeTask", "LLM服务分析执行结果失败", e);
                result.setLlmAnalysis("执行结果分析失败");
            }
            // 限制errorMessage长度，避免数据库错误
            if (result.getErrorMessage() != null && result.getErrorMessage().length() > 65535) {
                result.setErrorMessage(result.getErrorMessage().substring(0, 65535));
            }
            executionResultRepository.save(result);

            // 先保存结果再生成报告，这样报告能读取到完整数据
            // Report generation is handled after the result is persisted.
            persistAllureReport(taskId, result);
            executionResultRepository.save(result);

            task.setStatus(passed ? "COMPLETED" : "FAILED");
            executionTaskRepository.save(task);
            return result;
        } catch (Exception exception) {
            String requestUrl = parsedReq != null && parsedReq.path != null
                ? "http://127.0.0.1:" + serverPort + parsedReq.path
                : "http://127.0.0.1:" + serverPort + "/api/auth/login";
            String requestMethod = parsedReq != null ? parsedReq.method : "POST";
            String requestBody = parsedReq != null && parsedReq.body != null
                ? parsedReq.body : "{\"username\":\"admin\",\"password\":\"1\"}";

            result.setRequestUrl(requestUrl);
            result.setRequestMethod(requestMethod);
            result.setRequestHeaders("{\"Content-Type\":\"application/json\"}");
            result.setRequestBody(requestBody);
            result.setResponseStatus(500);
            result.setResponseBody("");
            result.setAssertionResult(expectedResultText != null
                ? expectedResultText : "接口应正常响应");
            result.setResult("FAILED");
            String errorMsg = exception.getMessage();
            // 限制errorMessage长度，避免数据库错误
            if (errorMsg != null && errorMsg.length() > 65535) {
                errorMsg = errorMsg.substring(0, 65535);
            }
            result.setErrorMessage(errorMsg);
            result.setLlmAnalysis("Execution failed before a valid response was returned.");
            executionResultRepository.save(result);
            persistAllureReport(taskId, result);

            task.setStatus("FAILED");
            executionTaskRepository.save(task);
            return result;
        }
    }

    public ExecutionResult getExecutionResult(Long taskId) {
        return executionResultRepository.findByTaskId(taskId);
    }

    public ExecutionTask retryTask(Long taskId) {
        ExecutionTask task = executionTaskRepository.findById(taskId).orElse(null);
        if (task == null) {
            return null;
        }

        ExecutionTask newTask = new ExecutionTask();
        newTask.setProjectId(task.getProjectId());
        newTask.setTestCaseId(task.getTestCaseId());
        newTask.setPlanId(task.getPlanId());
        newTask.setEnvironment(task.getEnvironment());
        newTask = executionTaskRepository.save(newTask);
        executeTask(newTask.getId());
        return newTask;
    }

    public List<ExecutionTask> createBatchTasks(Long projectId, List<Long> testCaseIds, String environment) {
        List<ExecutionTask> tasks = new ArrayList<>();
        for (Long testCaseId : testCaseIds) {
            TestCase testCase = testCaseRepository.findById(testCaseId).orElse(null);
            boolean executable = testCase != null && (
                TestMetadata.isInterfaceTest(testCase.getTestType(), testCase.getExecutionType(), testCase.getRequestData())
                    || TestMetadata.isWebTest(testCase.getTestType(), testCase.getExecutionType())
            );
            if (executable) {
                ExecutionTask task = new ExecutionTask();
                task.setProjectId(projectId);
                task.setTestCaseId(testCaseId);
                task.setPlanId(1L);
                task.setEnvironment(environment);
                task.setTaskType(testCase.getTestType());
                task.setRunnerType(TestMetadata.isWebTest(testCase.getTestType(), testCase.getExecutionType()) ? "WEB" : "LOCAL");
                tasks.add(executionTaskRepository.save(task));
            }
        }
        return tasks;
    }

    public List<ExecutionResult> executeBatchTasks(List<Long> taskIds) {
        List<ExecutionResult> results = new ArrayList<>();
        for (Long taskId : taskIds) {
            ExecutionResult result = executeTask(taskId);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    private ExecutionResult executeWebTask(ExecutionTask task, ExecutionResult result, TestCase testCase) {
        result.setTestType(TestMetadata.TEST_TYPE_WEB);
        if (useRealWebExecutor()) {
            return executeRealWebTask(task, result, testCase);
        }

        try {
            JsonNode payload = parseAutomationPayload(testCase.getAutomationPayload());
            String startUrl = payload.path("startUrl").asText("/");
            String finalUrl = payload.path("finalUrl").asText(startUrl);
            String finalAssertion = payload.path("finalAssertion").asText("页面操作执行成功");

            result.setRequestMethod("WEB");
            result.setRequestUrl(buildWebUrl(startUrl));
            result.setRequestHeaders("{\"runnerType\":\"WEB\",\"environment\":\"" + safeText(task.getEnvironment()) + "\"}");
            result.setRequestBody(prettyJson(payload));
            result.setResponseStatus(200);
            result.setResponseBody(buildWebExecutionSummary(payload, finalUrl));
            result.setAssertionResult(buildWebAssertionSummary(payload, finalAssertion));
            result.setResult("PASSED");
            result.setErrorMessage(null);

            try {
                String llmResult = llmService.analyzeExecutionResult(result.getResponseBody(), testCase.getSteps(), testCase.getExpectedResult());
                result.setLlmAnalysis(limitText(llmResult));
            } catch (Exception e) {
                LLMLogger.error("AutomationService", "executeWebTask", "LLM analysis for web execution failed", e);
                result.setLlmAnalysis("Web automation skeleton executed. LLM analysis unavailable.");
            }

            executionResultRepository.save(result);
            persistAllureReport(task.getId(), result);
            task.setStatus("COMPLETED");
            executionTaskRepository.save(task);
            return executionResultRepository.save(result);
        } catch (Exception exception) {
            result.setRequestMethod("WEB");
            result.setRequestUrl(buildWebUrl("/"));
            result.setRequestHeaders("{\"runnerType\":\"WEB\"}");
            result.setRequestBody(testCase.getAutomationPayload());
            result.setResponseStatus(500);
            result.setResponseBody("");
            result.setAssertionResult(testCase.getExpectedResult());
            result.setResult("FAILED");
            result.setErrorMessage(limitText(exception.getMessage()));
            result.setLlmAnalysis("Web automation skeleton failed before execution summary was produced.");
            executionResultRepository.save(result);
            persistAllureReport(task.getId(), result);
            task.setStatus("FAILED");
            executionTaskRepository.save(task);
            return executionResultRepository.save(result);
        }
    }

    // ======================== 测试步骤解析 ========================

    /**
     * 解析后的请求参数
     */
    private static class ParsedRequest {
        String method = "POST";
        String path = null;
        String contentType = "application/json";
        String body = "";
    }

    /**
     * 从 TestCase 的 requestData 字段中解析请求参数（结构化数据，优先级最高）
     */
    private ParsedRequest parseRequestData(TestCase testCase) {
        if (testCase == null || testCase.getRequestData() == null || testCase.getRequestData().trim().isEmpty()) {
            return null;
        }

        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(testCase.getRequestData());
            if (root.isNull() || root.isMissingNode()) {
                return null;
            }

            ParsedRequest req = new ParsedRequest();

            String method = root.path("method").asText(null);
            if (method != null) {
                req.method = method.toUpperCase();
            }

            String path = root.path("path").asText(null);
            if (path != null) {
                req.path = path;
            }

            String contentType = root.path("contentType").asText(null);
            if (contentType != null) {
                req.contentType = contentType;
            }

            com.fasterxml.jackson.databind.JsonNode bodyNode = root.path("body");
            if (!bodyNode.isNull() && !bodyNode.isMissingNode()) {
                req.body = bodyNode.toString();
            }

            return req.path != null ? req : null;
        } catch (Exception e) {
            LLMLogger.error("AutomationService", "parseRequestData", "解析 requestData 失败", e);
            return null;
        }
    }

    /**
     * 从测试步骤文本中解析出 HTTP 请求参数（method、path、body、Content-Type）
     * 支持的步骤格式示例：
     *   1. 设置请求头为 Content-Type 为 application/json
     *   2. 构造请求体 {"username": "admin", "password": "wrong_password"}
     *   3. 发送 POST 请求至 /api/auth/login
     */
    private ParsedRequest parseTestSteps(String steps) {
        if (steps == null || steps.trim().isEmpty()) {
            return null;
        }

        ParsedRequest req = new ParsedRequest();
        boolean foundPath = false;
        java.util.LinkedHashMap<String, String> bodyParams = new java.util.LinkedHashMap<>();

        for (String line : steps.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 1) 提取 HTTP method 和请求路径
            Matcher methodMatcher = Pattern.compile(
                "(GET|POST|PUT|DELETE|PATCH)", Pattern.CASE_INSENSITIVE
            ).matcher(line);
            if (methodMatcher.find() && (line.contains("请求") || line.contains("发送"))) {
                req.method = methodMatcher.group(1).toUpperCase();
                Matcher pathMatcher = Pattern.compile("(/[^\\s\"'，。]+)").matcher(line);
                if (pathMatcher.find()) {
                    req.path = pathMatcher.group(1);
                    foundPath = true;
                }
            }

            // 2) 提取请求体 JSON - 优先匹配显式 JSON 块
            if (line.contains("{") && (line.contains("请求体") || line.contains("body") 
                    || line.contains("构造") || line.contains("参数"))) {
                int braceStart = line.indexOf('{');
                req.body = extractJsonBlock(line, braceStart);
            }

            // 3) 智能提取参数：扫描所有包含英文字段名的行
            bodyParams.putAll(extractParamsFromLine(line));

            // 4) 提取 Content-Type
            if (line.toLowerCase().contains("content-type")) {
                Matcher ctMatcher = Pattern.compile(
                    "(application/[\\w.+-]+|text/[\\w.+-]+|multipart/[\\w.+-]+)",
                    Pattern.CASE_INSENSITIVE
                ).matcher(line);
                if (ctMatcher.find()) {
                    req.contentType = ctMatcher.group(1);
                }
            }
        }

        // 如果 body 还没有设置，但提取到了参数，则构建 JSON
        if ((req.body == null || req.body.isEmpty()) && !bodyParams.isEmpty()) {
            req.body = buildJsonFromParams(bodyParams);
        }

        return foundPath ? req : null;
    }

    /**
     * 从单行文本中智能提取参数键值对
     * 支持各种格式：
     * - "username 为 admin"
     * - "username 字段值为 admin"
     * - "包含正确的 username 字段值为 admin"
     * - "password 字段值 1"
     * - "username: admin"
     * - "username=admin"
     */
    private java.util.LinkedHashMap<String, String> extractParamsFromLine(String text) {
        java.util.LinkedHashMap<String, String> params = new java.util.LinkedHashMap<>();
        
        // 策略1: 匹配 "字段名 + 任意中文 + 为/是/等于/= + 值" 格式
        // 例如: "username 字段值为 admin", "包含正确的 username 字段值为 admin"
        Matcher pattern1 = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:[^a-zA-Z0-9_]*?[\\u4e00-\\u9fa5]*)*?[\\s]*(?:为|是|等于|:|=)\\s*([^\\s和，,。\"'{}\\u4e00-\\u9fa5]+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        while (pattern1.find()) {
            String key = pattern1.group(1);
            String value = pattern1.group(2);
            if (isValidParamValue(key, value)) {
                params.put(key, value);
            }
        }

        // 策略2: 匹配 "字段值 + 为/是 + 具体值" 格式（值在字段名后面）
        // 例如: "username 字段值 1", "password 字段值为 admin123"
        if (params.isEmpty()) {
            Matcher pattern2 = Pattern.compile(
                "([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?:字段|参数)*\\s*值*\\s*(?:为|是|:|=)?\\s*([^\\s和，,。\"'{}\\u4e00-\\u9fa5]+)",
                Pattern.CASE_INSENSITIVE
            ).matcher(text);
            while (pattern2.find()) {
                String key = pattern2.group(1);
                String value = pattern2.group(2);
                if (isValidParamValue(key, value)) {
                    params.put(key, value);
                }
            }
        }

        // 策略3: 匹配 "字段名 + 的值为 + 值" 格式
        // 例如: "username的值为admin", "password的值为123"
        if (params.isEmpty()) {
            Matcher pattern3 = Pattern.compile(
                "([a-zA-Z_][a-zA-Z0-9_]*)\\s*的\\s*值\\s*(?:为|是|:|=)?\\s*([^\\s和，,。\"'{}\\u4e00-\\u9fa5]+)",
                Pattern.CASE_INSENSITIVE
            ).matcher(text);
            while (pattern3.find()) {
                String key = pattern3.group(1);
                String value = pattern3.group(2);
                if (isValidParamValue(key, value)) {
                    params.put(key, value);
                }
            }
        }

        return params;
    }

    /**
     * 验证提取的参数值是否有效
     */
    private boolean isValidParamValue(String key, String value) {
        if (value.equals(key)) return false;
        if (value.matches(".*[\\u4e00-\\u9fa5]+.*")) return false;
        if (value.matches("^(?:字段|参数|值|的|为|是|包含|正确|错误|任意|请求|接口|响应|测试).*$")) return false;
        return true;
    }

    /**
     * 将提取的参数构建为 JSON 字符串
     */
    private String buildJsonFromParams(java.util.LinkedHashMap<String, String> params) {
        if (params.isEmpty()) return null;
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
            if (index > 0) json.append(", ");
            json.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            index++;
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 从文本中提取完整的 JSON 块（支持嵌套大括号）
     */
    private String extractJsonBlock(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            if (text.charAt(i) == '{') depth++;
            else if (text.charAt(i) == '}') {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        // 无法匹配闭合大括号，返回从 { 到行尾
        return text.substring(start);
    }

    /**
     * 从自然语言描述中提取 JSON 参数
     * 支持格式: "username 为 admin 和 password 为 1"
     * 支持格式: "username为admin, password为123456"
     */
    private String extractJsonFromNaturalLanguage(String text) {
        java.util.LinkedHashMap<String, String> params = new java.util.LinkedHashMap<>();
        // 匹配模式1: 字段名 + (可选中文) + 为/是/等于/= + (可选中文) + 值
        // 支持: "username 为 admin"
        // 支持: "username 字段值为 admin"
        // 支持: "包含正确的 username 字段值为 admin"
        // 支持: "password 字段值 1"
        Matcher paramMatcher = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*[^a-zA-Z0-9_]*?(?:为|是|等于|=|值)?\\s*[^a-zA-Z0-9_]*?([^\\s和，,。\"'{}]+)",
            Pattern.CASE_INSENSITIVE
        ).matcher(text);
        while (paramMatcher.find()) {
            String key = paramMatcher.group(1);
            String value = paramMatcher.group(2);
            // 跳过值等于key的情况（说明没有匹配到实际值）
            if (value.equals(key)) continue;
            // 跳过值包含中文字符的情况（说明匹配到了中文描述而非实际值）
            if (value.matches(".*[\\u4e00-\\u9fa5]+.*")) continue;
            // 跳过常见的非参数字
            if (key.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                params.put(key, value);
            }
        }
        if (params.isEmpty()) {
            return null;
        }
        // 构建 JSON 字符串
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
            if (index > 0) json.append(", ");
            json.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
            index++;
        }
        json.append("}");
        return json.toString();
    }

    // ======================== HTTP 调用 ========================

    /**
     * 根据解析出的参数动态发起 HTTP 调用
     */
    private HttpResponse<String> callEndpoint(String method, String url, String contentType, String body)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", contentType);

        switch (method.toUpperCase()) {
            case "GET":
                builder.GET();
                break;
            case "DELETE":
                builder.DELETE();
                break;
            case "POST":
                builder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            case "PUT":
                builder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            default:
                // PATCH 等其他方法
                builder.method(method.toUpperCase(),
                    HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * 保留的默认登录接口调用（回退用）
     */
    private HttpResponse<String> callLoginEndpoint() throws IOException, InterruptedException {
        return callEndpoint("POST",
            "http://127.0.0.1:" + serverPort + "/api/auth/login",
            "application/json",
            "{\"username\":\"admin\",\"password\":\"1\"}");
    }

    // ======================== 结果断言 ========================

    /**
     * 根据预期结果文本逐条断言实际 HTTP 响应，返回是否全部通过。
     * assertionDetails 会逐行记录每条断言的 ✓/✗ 结果及原因。
     *
     * 支持的预期结果格式示例：
     *   1. 响应状态码为 200
     *   2. 响应体中 success 字段为 true
     *   3. 响应体中包含 authorization 字段且以 Basic 开头
     *   4. 响应体中 username 字段返回 admin
     *   5. 返回体中 success 为 false
     *   6. 请求返回状态码 401 或 400
     */
    private boolean evaluateResult(HttpResponse<String> response, String expectedResult,
                                    StringBuilder assertionDetails) {
        if (expectedResult == null || expectedResult.trim().isEmpty()) {
            boolean ok = response.statusCode() >= 200 && response.statusCode() < 300;
            assertionDetails.append("无预期结果定义，HTTP状态码: " + response.statusCode())
                .append(ok ? " → 通过" : " → 不通过");
            return ok;
        }

        String body = response.body() != null ? response.body() : "";
        int actualStatus = response.statusCode();
        boolean allPassed = true;
        int checkIndex = 0;

        for (String rawLine : expectedResult.split("\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            String displayLine = line;
            // 去掉行首编号，如 "1. " 或 "1、"
            line = line.replaceFirst("^\\d+[.、)）]\\s*", "");
            if (line.isEmpty()) continue;
            checkIndex++;

            boolean linePassed = true;
            String reason = "";

            // ---- Pattern 1: 状态码断言 ----
            // "响应状态码为 200" / "请求返回状态码 401 或 400"
            if (line.contains("状态码")) {
                List<Integer> expectedCodes = new ArrayList<>();
                Matcher codeMatcher = Pattern.compile("(\\d{3})").matcher(line);
                while (codeMatcher.find()) {
                    int code = Integer.parseInt(codeMatcher.group(1));
                    if (code >= 100 && code < 600) {
                        expectedCodes.add(code);
                    }
                }
                if (!expectedCodes.isEmpty()) {
                    if (expectedCodes.contains(actualStatus)) {
                        reason = "期望状态码 " + expectedCodes + "，实际 " + actualStatus + " → 匹配";
                    } else {
                        linePassed = false;
                        reason = "期望状态码 " + expectedCodes + "，实际 " + actualStatus + " → 不匹配";
                    }
                }
            }
            // ---- Pattern 2: 字段存在性 + 前缀检查 ----
            // "包含 authorization 字段且以 Basic 开头"
            else if (line.contains("包含") && line.contains("字段")) {
                Matcher containsMatcher = Pattern.compile("包含\\s*(\\w+)\\s*字段").matcher(line);
                if (containsMatcher.find()) {
                    String fieldName = containsMatcher.group(1);
                    if (!body.contains("\"" + fieldName + "\"")) {
                        linePassed = false;
                        reason = "响应体中不包含字段 " + fieldName;
                    } else {
                        reason = "字段 " + fieldName + " 存在";
                        // 检查 "以 XXX 开头"
                        if (line.contains("开头")) {
                            Matcher prefixMatcher = Pattern.compile("以\\s*(\\S+)\\s*开头").matcher(line);
                            if (prefixMatcher.find()) {
                                String prefix = prefixMatcher.group(1);
                                Matcher valueMatcher = Pattern.compile(
                                    "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\""
                                ).matcher(body);
                                if (valueMatcher.find()) {
                                    String actualValue = valueMatcher.group(1);
                                    if (actualValue.startsWith(prefix)) {
                                        reason += "，且值以 " + prefix + " 开头 → 匹配";
                                    } else {
                                        linePassed = false;
                                        String preview = actualValue.length() > 30
                                            ? actualValue.substring(0, 30) + "..." : actualValue;
                                        reason += "，但值不以 " + prefix + " 开头 (实际: " + preview + ")";
                                    }
                                } else {
                                    linePassed = false;
                                    reason += "，但无法提取字段值";
                                }
                            }
                        }
                    }
                }
            }
            // ---- Pattern 3: 字段值断言 ----
            // "success 字段为 true" / "username 字段返回 admin"
            else if (line.contains("字段")) {
                Matcher fieldMatcher = Pattern.compile(
                    "(\\w+)\\s*字段[为是返回]+\\s*(\\S+)"
                ).matcher(line);
                if (fieldMatcher.find()) {
                    String fieldName = fieldMatcher.group(1);
                    String expectedValue = fieldMatcher.group(2);
                    boolean matched = checkFieldValue(body, fieldName, expectedValue);
                    if (matched) {
                        reason = fieldName + "=" + expectedValue + " → 匹配";
                    } else {
                        linePassed = false;
                        String actualValue = extractFieldValue(body, fieldName);
                        reason = "期望 " + fieldName + "=" + expectedValue
                            + "，实际 " + (actualValue != null ? actualValue : "字段不存在") + " → 不匹配";
                    }
                }
            }
            // ---- Pattern 4: 通用 key 为/是 value ----
            // "success 为 false" / "返回体中 success 为 false"
            else {
                Matcher genericMatcher = Pattern.compile(
                    "(\\w+)\\s*[为是]\\s*(true|false|\\S+)", Pattern.CASE_INSENSITIVE
                ).matcher(line);
                if (genericMatcher.find()) {
                    String fieldName = genericMatcher.group(1);
                    String expectedValue = genericMatcher.group(2);
                    boolean matched = checkFieldValue(body, fieldName, expectedValue);
                    if (matched) {
                        reason = fieldName + "=" + expectedValue + " → 匹配";
                    } else {
                        linePassed = false;
                        String actualValue = extractFieldValue(body, fieldName);
                        reason = "期望 " + fieldName + "=" + expectedValue
                            + "，实际 " + (actualValue != null ? actualValue : "字段不存在") + " → 不匹配";
                    }
                } else if (line.contains("包含") || line.contains("错误") || line.contains("提示")) {
                    // 包含关键词的模糊检查，例如 "返回结果中包含相关错误提示信息"
                    // 这类断言无法精确校验，标记为信息项
                    reason = "语义断言，需人工或LLM判定";
                }
            }

            if (!linePassed) allPassed = false;
            assertionDetails.append(checkIndex).append(". ")
                .append(linePassed ? "✓ " : "✗ ")
                .append(displayLine);
            if (!reason.isEmpty()) {
                assertionDetails.append("  [").append(reason).append("]");
            }
            assertionDetails.append("\n");
        }

        assertionDetails.append("\n总体结果: ").append(allPassed ? "全部通过 ✓" : "存在不通过项 ✗");
        LLMLogger.info("AutomationService", "evaluateResult",
            "断言结果: " + (allPassed ? "PASSED" : "FAILED") + " (" + checkIndex + " 条检查)");
        return allPassed;
    }

    /**
     * 检查 JSON 响应体中某字段的值是否与期望值匹配
     */
    private boolean checkFieldValue(String body, String fieldName, String expectedValue) {
        // 支持多种 JSON 格式: "field":value / "field": value / "field":"value" / "field": "value"
        return body.contains("\"" + fieldName + "\":" + expectedValue)
            || body.contains("\"" + fieldName + "\": " + expectedValue)
            || body.contains("\"" + fieldName + "\":\"" + expectedValue + "\"")
            || body.contains("\"" + fieldName + "\": \"" + expectedValue + "\"");
    }

    /**
     * 从 JSON 响应体中提取字段的实际值（用于断言失败时显示）
     */
    private String extractFieldValue(String body, String fieldName) {
        // 尝试提取字符串值: "field":"value"
        Matcher strMatcher = Pattern.compile(
            "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\""
        ).matcher(body);
        if (strMatcher.find()) {
            return "\"" + strMatcher.group(1) + "\"";
        }
        // 尝试提取非字符串值: "field":true / "field":123
        Matcher valMatcher = Pattern.compile(
            "\"" + fieldName + "\"\\s*:\\s*([^,\\}\s]+)"
        ).matcher(body);
        if (valMatcher.find()) {
            return valMatcher.group(1);
        }
        return null;
    }

    // ======================== 摘要构建 ========================

    private String buildExecutionSummary(String url, String method, String body,
                                          HttpResponse<String> response) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request URL: " + url + "\n");
        sb.append("Request method: " + method + "\n");
        if (body != null && !body.isEmpty()) {
            sb.append("Request body: " + body + "\n");
        }
        sb.append("Response status: " + response.statusCode() + "\n");
        sb.append("Response body: " + response.body());
        return sb.toString();
    }

    // ======================== Allure 报告 ========================

    private void persistAllureReport(Long taskId, ExecutionResult result) {
        try {
            result.setReportUrl(allureReportService.generateReport(taskId));
        } catch (Exception exception) {
            String reportError = "Allure report generation failed: " + exception.getMessage();
            result.setErrorMessage(mergeMessages(result.getErrorMessage(), reportError));
        }
        executionResultRepository.save(result);
    }

    private String mergeMessages(String currentMessage, String newMessage) {
        if (currentMessage == null || currentMessage.isBlank()) {
            return newMessage;
        }
        if (newMessage == null || newMessage.isBlank()) {
            return currentMessage;
        }
        if (currentMessage.contains(newMessage)) {
            return currentMessage;
        }
        return currentMessage + " | " + newMessage;
    }

    // Web automation helpers
    private JsonNode parseAutomationPayload(String automationPayload) throws IOException {
        if (automationPayload == null || automationPayload.isBlank()) {
            throw new IllegalStateException("Web automation payload is empty.");
        }
        JsonNode payload = OBJECT_MAPPER.readTree(automationPayload);
        if (!payload.isObject() || !payload.path("steps").isArray() || payload.path("steps").isEmpty()) {
            throw new IllegalStateException("Web automation payload must contain a non-empty steps array.");
        }
        return payload;
    }

    private String buildWebUrl(String path) {
        if (path == null || path.isBlank()) {
            return "http://127.0.0.1:" + serverPort + "/";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        return "http://127.0.0.1:" + serverPort + normalized;
    }

    private String buildWebExecutionSummary(JsonNode payload, String finalUrl) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("Runner: WEB");
        lines.add("Start URL: " + buildWebUrl(payload.path("startUrl").asText("/")));
        lines.add("Final URL: " + buildWebUrl(finalUrl));
        lines.add("Step count: " + payload.path("steps").size());
        lines.add("");
        lines.add("Automation payload:");
        lines.add(prettyJson(payload));
        return String.join("\n", lines);
    }

    private String buildWebAssertionSummary(JsonNode payload, String finalAssertion) {
        StringBuilder summary = new StringBuilder();
        summary.append("1. 已解析 Web 自动化步骤数量：" + payload.path("steps").size() + "\n");
        int index = 2;
        Iterator<JsonNode> iterator = payload.path("steps").elements();
        while (iterator.hasNext()) {
            JsonNode step = iterator.next();
            summary.append(index++).append(". ")
                .append(step.path("action").asText("unknown"))
                .append(" - ")
                .append(step.path("description").asText("未提供步骤说明"))
                .append("\n");
        }
        summary.append(index).append(". 最终断言：" + finalAssertion);
        return summary.toString();
    }

    private String prettyJson(JsonNode node) throws IOException {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    private String safeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "'");
    }

    private String limitText(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 65535 ? value.substring(0, 65535) : value;
    }

    private void normalizeTask(ExecutionTask task) {
        if (task == null) {
            return;
        }
        task.setTaskType(task.getTaskType());
    }
}