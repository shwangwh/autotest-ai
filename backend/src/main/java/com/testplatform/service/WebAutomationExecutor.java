package com.testplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.testplatform.entity.ExecutionResult;
import com.testplatform.entity.ExecutionTask;
import com.testplatform.entity.TestCase;
import com.testplatform.repository.ExecutionResultRepository;
import com.testplatform.repository.ExecutionTaskRepository;
import com.testplatform.util.LLMLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class WebAutomationExecutor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Path WEB_ARTIFACTS_ROOT = Path.of("automation-artifacts");

    private final ExecutionResultRepository executionResultRepository;
    private final ExecutionTaskRepository executionTaskRepository;
    private final AllureReportService allureReportService;
    private final LLMService llmService;
    private final String defaultWebBaseUrl;
    private final String productionWebBaseUrl;
    private final String stagingWebBaseUrl;
    private final String devWebBaseUrl;

    public WebAutomationExecutor(
        ExecutionResultRepository executionResultRepository,
        ExecutionTaskRepository executionTaskRepository,
        AllureReportService allureReportService,
        LLMService llmService,
        @Value("${web.automation.base-url.default:http://127.0.0.1:8082}") String defaultWebBaseUrl,
        @Value("${web.automation.base-url.production:http://127.0.0.1:8082}") String productionWebBaseUrl,
        @Value("${web.automation.base-url.staging:http://127.0.0.1:8082}") String stagingWebBaseUrl,
        @Value("${web.automation.base-url.dev:http://127.0.0.1:8082}") String devWebBaseUrl
    ) {
        this.executionResultRepository = executionResultRepository;
        this.executionTaskRepository = executionTaskRepository;
        this.allureReportService = allureReportService;
        this.llmService = llmService;
        this.defaultWebBaseUrl = normalizeBaseUrl(defaultWebBaseUrl);
        this.productionWebBaseUrl = normalizeBaseUrl(productionWebBaseUrl);
        this.stagingWebBaseUrl = normalizeBaseUrl(stagingWebBaseUrl);
        this.devWebBaseUrl = normalizeBaseUrl(devWebBaseUrl);
    }

    public ExecutionResult execute(ExecutionTask task, ExecutionResult result, TestCase testCase) {
        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;
        JsonNode payload = null;
        Path artifactDir = null;
        List<Map<String, Object>> executedSteps = new ArrayList<>();
        Map<String, Object> executionArtifacts = new LinkedHashMap<>();
        String startUrl = "/";
        String baseUrl = defaultWebBaseUrl;

        try {
            payload = parseAutomationPayload(testCase.getAutomationPayload());
            startUrl = payload.path("startUrl").asText("/");
            baseUrl = resolveWebBaseUrl(task.getEnvironment(), payload);
            String initialUrl = resolveWebUrl(baseUrl, startUrl);
            artifactDir = prepareArtifactDirectory(task.getId());

            result.setRequestMethod("WEB");
            result.setRequestUrl(initialUrl);
            result.setRequestHeaders(OBJECT_MAPPER.writeValueAsString(Map.of(
                "runnerType", "WEB",
                "environment", defaultText(task.getEnvironment(), "PRODUCTION"),
                "baseUrl", baseUrl,
                "browser", "msedge"
            )));
            result.setRequestBody(prettyJson(payload));

            playwright = Playwright.create();
            browser = launchBrowser(playwright);
            context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1440, 900)
                .setIgnoreHTTPSErrors(true));
            page = context.newPage();
            page.setDefaultTimeout(resolveTimeout(payload));
            page.setDefaultNavigationTimeout(resolveTimeout(payload));

            int stepIndex = 1;
            for (JsonNode step : payload.path("steps")) {
                executedSteps.add(executeBrowserStep(page, step, stepIndex++, baseUrl, artifactDir));
            }

            Map<String, Object> finalState = capturePageState(page);
            String finalScreenshot = captureScreenshot(page, artifactDir, "final-page");
            if (finalScreenshot != null) {
                finalState.put("finalScreenshot", finalScreenshot);
            }
            validateFinalState(page, payload);

            result.setResponseStatus(200);
            result.setResponseBody(limitText(buildWebExecutionSummary(baseUrl, payload, executedSteps, finalState)));
            result.setAssertionResult(limitText(buildWebAssertionSummary(executedSteps, payload)));
            result.setResult("PASSED");
            result.setErrorMessage(null);

            executionArtifacts.put("baseUrl", baseUrl);
            executionArtifacts.put("startUrl", initialUrl);
            executionArtifacts.put("steps", executedSteps);
            executionArtifacts.put("pageState", finalState);
            executionArtifacts.put("status", "PASSED");
            executionArtifacts.put("failureStep", null);
            result.setArtifacts(limitText(OBJECT_MAPPER.writeValueAsString(executionArtifacts)));

            try {
                String llmResult = llmService.analyzeExecutionResult(
                    result.getResponseBody(),
                    testCase.getSteps(),
                    testCase.getExpectedResult()
                );
                result.setLlmAnalysis(limitText(llmResult));
            } catch (Exception e) {
                LLMLogger.error("WebAutomationExecutor", "execute", "LLM analysis for web execution failed", e);
                result.setLlmAnalysis("Web automation executed successfully, but LLM analysis is unavailable.");
            }

            executionResultRepository.save(result);
            persistAllureReport(task.getId(), result);
            task.setStatus("COMPLETED");
            executionTaskRepository.save(task);
            return executionResultRepository.save(result);
        } catch (Exception exception) {
            Map<String, Object> pageState = capturePageStateSafely(page);
            String failureScreenshot = captureScreenshot(page, artifactDir, "failure-page");
            String failureStep = executedSteps.isEmpty()
                ? "No browser step completed."
                : defaultText((String) executedSteps.get(executedSteps.size() - 1).get("name"), "Unknown step");

            if (failureScreenshot != null) {
                pageState.put("failureScreenshot", failureScreenshot);
            }

            executionArtifacts.put("baseUrl", baseUrl);
            executionArtifacts.put("startUrl", resolveWebUrl(baseUrl, startUrl));
            executionArtifacts.put("steps", executedSteps);
            executionArtifacts.put("pageState", pageState);
            executionArtifacts.put("status", "FAILED");
            executionArtifacts.put("failureStep", failureStep);
            executionArtifacts.put("error", limitText(exception.getMessage()));

            result.setRequestMethod("WEB");
            result.setRequestUrl(resolveWebUrl(baseUrl, startUrl));
            result.setRequestHeaders(buildFallbackWebHeaders(task, baseUrl));
            result.setRequestBody(testCase.getAutomationPayload());
            result.setResponseStatus(exception instanceof TimeoutError ? 408 : 500);
            result.setResponseBody(limitText(buildFailureSummary(executedSteps, pageState, exception)));
            result.setAssertionResult(limitText(buildFailureAssertionSummary(executedSteps, payload, exception)));
            result.setResult("FAILED");
            result.setErrorMessage(limitText(exception.getMessage()));
            result.setArtifacts(writeArtifactsQuietly(executionArtifacts));
            result.setLlmAnalysis("Web automation execution failed before the expected page assertion passed.");
            executionResultRepository.save(result);
            persistAllureReport(task.getId(), result);
            task.setStatus("FAILED");
            executionTaskRepository.save(task);
            return executionResultRepository.save(result);
        } finally {
            closeQuietly(page);
            closeQuietly(context);
            closeQuietly(browser);
            closeQuietly(playwright);
        }
    }

    private Browser launchBrowser(Playwright playwright) {
        try {
            return playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setChannel("msedge")
                .setHeadless(true));
        } catch (RuntimeException exception) {
            LLMLogger.error("WebAutomationExecutor", "launchBrowser", "Failed to launch Edge channel, fallback to bundled Chromium", exception);
            return playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        }
    }

    private Map<String, Object> executeBrowserStep(Page page, JsonNode step, int index, String baseUrl, Path artifactDir) {
        String action = step.path("action").asText("unknown").trim();
        String name = index + ". " + step.path("description").asText(action);
        long startedAt = System.currentTimeMillis();
        Map<String, Object> artifact = new LinkedHashMap<>();
        artifact.put("index", index);
        artifact.put("name", name);
        artifact.put("action", action);
        artifact.put("target", nullableText(step, "target"));
        artifact.put("selector", nullableText(step, "selector"));
        artifact.put("value", nullableText(step, "value"));

        try {
            runBrowserAction(page, step, action, baseUrl);
            if (shouldCaptureStep(step, action)) {
                artifact.put("screenshot", captureScreenshot(page, artifactDir, "step-" + index));
            }
            artifact.put("status", "PASSED");
            artifact.put("pageUrl", page.url());
        } catch (Exception exception) {
            artifact.put("status", "FAILED");
            artifact.put("pageUrl", safeGetPageUrl(page));
            artifact.put("error", limitText(exception.getMessage()));
            String screenshot = captureScreenshot(page, artifactDir, "step-" + index + "-failed");
            if (screenshot != null) {
                artifact.put("screenshot", screenshot);
            }
            artifact.put("pageState", capturePageStateSafely(page));
            artifact.put("startedAt", startedAt);
            artifact.put("finishedAt", System.currentTimeMillis());
            throw exception;
        }

        artifact.put("startedAt", startedAt);
        artifact.put("finishedAt", System.currentTimeMillis());
        return artifact;
    }

    private void runBrowserAction(Page page, JsonNode step, String action, String baseUrl) {
        switch (action.toLowerCase(Locale.ROOT)) {
            case "goto" -> page.navigate(resolveWebUrl(baseUrl, readTarget(step)));
            case "click" -> page.locator(requireField(step, "selector")).click();
            case "fill" -> page.locator(requireField(step, "selector")).fill(readValue(step));
            case "press" -> page.locator(requireField(step, "selector")).press(readValue(step));
            case "waitforselector" -> page.locator(requireField(step, "selector"))
                .waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(readTimeout(step)));
            case "waitforvisible", "waitvisible" -> page.locator(requireField(step, "selector"))
                .waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(readTimeout(step)));
            case "waitfortimeout" -> page.waitForTimeout(readTimeout(step));
            case "waiturlcontains", "waitforurlcontains" -> waitUrlContains(page, readUrlFragment(step), readTimeout(step));
            case "asserturlcontains" -> assertUrlContains(page, readUrlFragment(step));
            case "asserttextcontains", "verifytext" -> assertTextContains(page, requireField(step, "selector"), readValue(step));
            case "assertvisible" -> assertVisible(page, requireField(step, "selector"));
            case "hover" -> page.locator(requireField(step, "selector")).hover();
            case "select" -> page.locator(requireField(step, "selector")).selectOption(readValue(step));
            case "check" -> page.locator(requireField(step, "selector")).check();
            case "uncheck" -> page.locator(requireField(step, "selector")).uncheck();
            case "screenshot" -> {
            }
            default -> throw new IllegalStateException("Unsupported web action: " + action);
        }
    }

    private void validateFinalState(Page page, JsonNode payload) {
        String finalUrl = payload.path("finalUrl").asText("");
        if (!finalUrl.isBlank()) {
            assertUrlContains(page, finalUrl);
        }

        String finalAssertion = payload.path("finalAssertion").asText("");
        if (finalAssertion.toLowerCase(Locale.ROOT).contains("url") && finalAssertion.contains("/")) {
            assertUrlContains(page, finalAssertion.substring(finalAssertion.indexOf('/')));
        }
    }

    private Map<String, Object> capturePageState(Page page) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("url", page.url());
        state.put("title", page.title());
        state.put("bodyText", limitText(page.locator("body").innerText()));
        state.put("dom", limitText(page.content()));
        return state;
    }

    private Map<String, Object> capturePageStateSafely(Page page) {
        try {
            return page == null ? new LinkedHashMap<>() : capturePageState(page);
        } catch (Exception exception) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("error", limitText(exception.getMessage()));
            fallback.put("url", safeGetPageUrl(page));
            return fallback;
        }
    }

    private String captureScreenshot(Page page, Path artifactDir, String fileName) {
        if (page == null || artifactDir == null) {
            return null;
        }
        try {
            Path target = artifactDir.resolve(fileName + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(target).setFullPage(true));
            return target.toAbsolutePath().toString();
        } catch (Exception exception) {
            LLMLogger.error("WebAutomationExecutor", "captureScreenshot", "Failed to capture screenshot", exception);
            return null;
        }
    }

    private Path prepareArtifactDirectory(Long taskId) {
        try {
            Path directory = WEB_ARTIFACTS_ROOT.resolve(String.valueOf(taskId));
            Files.createDirectories(directory);
            return directory;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare web artifact directory.", exception);
        }
    }

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

    private long resolveTimeout(JsonNode payload) {
        return payload.path("timeout").asLong(15000L);
    }

    private boolean shouldCaptureStep(JsonNode step, String action) {
        return step.path("capture").asBoolean(false) || "screenshot".equalsIgnoreCase(action);
    }

    private String resolveWebBaseUrl(String environment, JsonNode payload) {
        String configured = payload.path("baseUrl").asText("");
        if (!configured.isBlank()) {
            return normalizeBaseUrl(configured);
        }
        String env = defaultText(environment, "PRODUCTION").toUpperCase(Locale.ROOT);
        return switch (env) {
            case "DEV" -> devWebBaseUrl;
            case "STAGING" -> stagingWebBaseUrl;
            case "PRODUCTION" -> productionWebBaseUrl;
            default -> defaultWebBaseUrl;
        };
    }

    private String resolveWebUrl(String baseUrl, String path) {
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizeBaseUrl(baseUrl) + normalizedPath;
    }

    private String buildFallbackWebHeaders(ExecutionTask task, String baseUrl) {
        try {
            return OBJECT_MAPPER.writeValueAsString(Map.of(
                "runnerType", "WEB",
                "environment", defaultText(task == null ? null : task.getEnvironment(), "PRODUCTION"),
                "baseUrl", baseUrl
            ));
        } catch (Exception exception) {
            return "{\"runnerType\":\"WEB\"}";
        }
    }

    private String buildWebExecutionSummary(
        String baseUrl,
        JsonNode payload,
        List<Map<String, Object>> executedSteps,
        Map<String, Object> finalState
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("Runner: WEB");
        lines.add("Base URL: " + baseUrl);
        lines.add("Start URL: " + resolveWebUrl(baseUrl, payload.path("startUrl").asText("/")));
        lines.add("Executed steps: " + executedSteps.size());
        lines.add("Final URL: " + defaultText((String) finalState.get("url"), "N/A"));
        lines.add("Final title: " + defaultText((String) finalState.get("title"), "N/A"));
        lines.add("");
        for (Map<String, Object> step : executedSteps) {
            lines.add(step.get("name") + " -> " + step.get("status"));
        }
        return String.join("\n", lines);
    }

    private String buildWebAssertionSummary(List<Map<String, Object>> executedSteps, JsonNode payload) {
        StringBuilder summary = new StringBuilder();
        summary.append("Executed ").append(executedSteps.size()).append(" web step(s).\n");
        for (Map<String, Object> step : executedSteps) {
            summary.append(step.get("name"))
                .append(": ")
                .append(step.get("status"))
                .append("\n");
        }
        String finalUrl = payload.path("finalUrl").asText("");
        if (!finalUrl.isBlank()) {
            summary.append("Final URL assertion: contains ").append(finalUrl).append("\n");
        }
        String finalAssertion = payload.path("finalAssertion").asText("");
        if (!finalAssertion.isBlank()) {
            summary.append("Final assertion: ").append(finalAssertion);
        }
        return summary.toString().trim();
    }

    private String buildFailureSummary(List<Map<String, Object>> executedSteps, Map<String, Object> pageState, Exception exception) {
        List<String> lines = new ArrayList<>();
        lines.add("Runner: WEB");
        lines.add("Status: FAILED");
        lines.add("Error: " + defaultText(exception.getMessage(), exception.getClass().getSimpleName()));
        lines.add("Executed steps: " + executedSteps.size());
        lines.add("Current URL: " + defaultText((String) pageState.get("url"), "N/A"));
        lines.add("Current title: " + defaultText((String) pageState.get("title"), "N/A"));
        return String.join("\n", lines);
    }

    private String buildFailureAssertionSummary(List<Map<String, Object>> executedSteps, JsonNode payload, Exception exception) {
        StringBuilder summary = new StringBuilder();
        for (Map<String, Object> step : executedSteps) {
            summary.append(step.get("name"))
                .append(": ")
                .append(step.get("status"));
            if (step.get("error") != null) {
                summary.append(" (").append(step.get("error")).append(")");
            }
            summary.append("\n");
        }
        if (payload != null && payload.path("finalAssertion").isTextual()) {
            summary.append("Expected final assertion: ").append(payload.path("finalAssertion").asText()).append("\n");
        }
        summary.append("Failure: ").append(defaultText(exception.getMessage(), exception.getClass().getSimpleName()));
        return summary.toString().trim();
    }

    private String writeArtifactsQuietly(Map<String, Object> executionArtifacts) {
        try {
            return OBJECT_MAPPER.writeValueAsString(executionArtifacts);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void assertUrlContains(Page page, String expected) {
        if (expected == null || expected.isBlank()) {
            return;
        }
        String currentUrl = page.url();
        if (!currentUrl.contains(expected)) {
            throw new IllegalStateException("Expected URL to contain '" + expected + "' but was '" + currentUrl + "'");
        }
    }

    private void waitUrlContains(Page page, String expected, long timeoutMs) {
        if (expected == null || expected.isBlank()) {
            return;
        }

        long deadline = System.currentTimeMillis() + Math.max(timeoutMs, 0L);
        while (System.currentTimeMillis() <= deadline) {
            if (page.url().contains(expected)) {
                return;
            }
            page.waitForTimeout(100);
        }
        assertUrlContains(page, expected);
    }

    private void assertTextContains(Page page, String selector, String expected) {
        String actual = page.locator(selector).innerText();
        if (expected == null || expected.isBlank()) {
            throw new IllegalStateException("Assertion value is empty for selector " + selector);
        }
        if (actual == null || !actual.contains(expected)) {
            throw new IllegalStateException("Expected text '" + expected + "' was not found in selector " + selector);
        }
    }

    private void assertVisible(Page page, String selector) {
        if (!page.locator(selector).isVisible()) {
            throw new IllegalStateException("Expected selector to be visible: " + selector);
        }
    }

    private String readTarget(JsonNode step) {
        String target = nullableText(step, "target");
        if (target == null) {
            target = nullableText(step, "url");
        }
        return defaultText(target, "/");
    }

    private String readValue(JsonNode step) {
        return defaultText(nullableText(step, "value"), "");
    }

    private String readUrlFragment(JsonNode step) {
        String value = nullableText(step, "value");
        if (value == null || value.isBlank()) {
            value = nullableText(step, "target");
        }
        if (value == null || value.isBlank()) {
            value = nullableText(step, "url");
        }
        return defaultText(value, "");
    }

    private long readTimeout(JsonNode step) {
        return step.path("timeout").asLong(1000L);
    }

    private String requireField(JsonNode step, String fieldName) {
        String value = nullableText(step, fieldName);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Web automation step missing field: " + fieldName);
        }
        return value;
    }

    private String nullableText(JsonNode node, String fieldName) {
        JsonNode child = node.path(fieldName);
        return child.isMissingNode() || child.isNull() ? null : child.asText();
    }

    private String prettyJson(JsonNode node) throws IOException {
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String candidate = defaultText(baseUrl, "http://127.0.0.1:8082").trim();
        return candidate.endsWith("/") ? candidate.substring(0, candidate.length() - 1) : candidate;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeGetPageUrl(Page page) {
        try {
            return page == null ? null : page.url();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String limitText(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 65535 ? value.substring(0, 65535) : value;
    }

    private void persistAllureReport(Long taskId, ExecutionResult result) {
        try {
            result.setReportUrl(allureReportService.generateReport(taskId));
        } catch (Exception exception) {
            String reportError = "Allure report generation failed: " + exception.getMessage();
            result.setErrorMessage(result.getErrorMessage() == null
                ? reportError
                : result.getErrorMessage() + " | " + reportError);
        }
        executionResultRepository.save(result);
    }

    private void closeQuietly(Page page) {
        try {
            if (page != null) {
                page.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(BrowserContext context) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(Browser browser) {
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(Playwright playwright) {
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (Exception ignored) {
        }
    }
}
