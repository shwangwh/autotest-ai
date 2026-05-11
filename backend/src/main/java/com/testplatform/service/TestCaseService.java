package com.testplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.TestCase;
import com.testplatform.entity.TestPoint;
import com.testplatform.repository.TestCaseRepository;
import com.testplatform.repository.TestPointRepository;
import com.testplatform.util.LLMLogger;
import com.testplatform.util.TestMetadata;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TestCaseService {
    private static final int MAX_TEST_CASE_TITLE_LENGTH = 200;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TestCaseRepository testCaseRepository;
    private final TestPointRepository testPointRepository;
    private final LLMService llmService;

    public TestCaseService(TestCaseRepository testCaseRepository, TestPointRepository testPointRepository, LLMService llmService) {
        this.testCaseRepository = testCaseRepository;
        this.testPointRepository = testPointRepository;
        this.llmService = llmService;
    }

    public List<TestCase> getAllTestCases() {
        List<TestCase> testCases = testCaseRepository.findAll();
        testCases.forEach(this::normalizeTestCase);
        return testCases;
    }

    public List<TestCase> getTestCasesByProjectId(Long projectId) {
        List<TestCase> testCases = testCaseRepository.findByProjectId(projectId);
        testCases.forEach(this::normalizeTestCase);
        return testCases;
    }

    public List<TestCase> getTestCasesByTestPointId(Long testPointId) {
        List<TestCase> testCases = testCaseRepository.findByTestPointId(testPointId);
        testCases.forEach(this::normalizeTestCase);
        return testCases;
    }

    public TestCase getTestCaseById(Long id) {
        TestCase testCase = testCaseRepository.findById(id).orElse(null);
        normalizeTestCase(testCase);
        return testCase;
    }

    public List<TestCase> generateTestCases(Long projectId, Long testPointId) {
        return generateTestCases(projectId, testPointId, null);
    }

    public List<TestCase> generateTestCases(Long projectId, Long testPointId, String prompt) {
        List<TestCase> existing = testCaseRepository.findByTestPointId(testPointId);
        if (!existing.isEmpty()) {
            testCaseRepository.deleteAll(existing);
        }

        TestPoint testPoint = testPointRepository.findById(testPointId).orElse(null);
        if (testPoint == null) {
            return List.of();
        }

        String testType = TestMetadata.normalizeTestType(testPoint.getTestType());
        String llmResult = generateTestCasesSafely(testPoint, prompt);
        List<TestCase> generatedCases = parseTestCases(projectId, testPointId, testPoint.getName(), llmResult, testType);
        if (generatedCases.isEmpty()) {
            generatedCases = createDefaultCases(projectId, testPointId, testPoint.getName(), testType);
        }

        try {
            generatedCases.forEach(this::normalizeTestCase);
            return testCaseRepository.saveAll(generatedCases);
        } catch (Exception exception) {
            LLMLogger.error("TestCaseService", "generateTestCases", "Failed to save generated test cases, falling back to defaults.", exception);
            List<TestCase> defaultCases = createDefaultCases(projectId, testPointId, testPoint.getName(), testType);
            defaultCases.forEach(this::normalizeTestCase);
            return testCaseRepository.saveAll(defaultCases);
        }
    }

    public TestCase updateTestCase(Long id, TestCase testCase) {
        TestCase existingTestCase = testCaseRepository.findById(id).orElse(null);
        if (existingTestCase == null) {
            return null;
        }

        existingTestCase.setTitle(testCase.getTitle());
        existingTestCase.setPrecondition(testCase.getPrecondition());
        existingTestCase.setSteps(testCase.getSteps());
        existingTestCase.setExpectedResult(testCase.getExpectedResult());
        existingTestCase.setPriority(testCase.getPriority());
        existingTestCase.setAutomation(testCase.getAutomation());
        existingTestCase.setStatus(testCase.getStatus());
        existingTestCase.setCaseType(testCase.getCaseType());
        existingTestCase.setTestType(testCase.getTestType());
        existingTestCase.setExecutionType(testCase.getExecutionType());
        existingTestCase.setRequestData(testCase.getRequestData());
        existingTestCase.setAutomationPayload(testCase.getAutomationPayload());
        existingTestCase.setScriptRefId(testCase.getScriptRefId());
        normalizeTestCase(existingTestCase);
        return testCaseRepository.save(existingTestCase);
    }

    public void deleteTestCase(Long id) {
        testCaseRepository.deleteById(id);
    }

    public TestCase addTestCase(TestCase testCase) {
        if (testCase.getCaseNumber() == null || testCase.getCaseNumber().isEmpty()) {
            String caseNumber = "TC-" + testCase.getTestPointId() + "-" + System.currentTimeMillis();
            testCase.setCaseNumber(caseNumber);
            testCase.setCaseCode(caseNumber);
        }
        if (testCase.getStatus() == null) {
            testCase.setStatus("PENDING_REVIEW");
        }
        if (testCase.getPriority() == null) {
            testCase.setPriority("MEDIUM");
        }
        if (testCase.getCreator() == null) {
            testCase.setCreator("admin");
        }
        if (testCase.getCreatedBy() == null) {
            testCase.setCreatedBy(1L);
        }
        normalizeTestCase(testCase);
        return testCaseRepository.save(testCase);
    }

    public List<TestCase> generateBatchTestCases(Long projectId, List<Long> testPointIds) {
        return generateBatchTestCases(projectId, testPointIds, null);
    }

    public List<TestCase> generateBatchTestCases(Long projectId, List<Long> testPointIds, String prompt) {
        List<TestCase> allGeneratedCases = new ArrayList<>();
        for (Long testPointId : testPointIds) {
            allGeneratedCases.addAll(generateTestCases(projectId, testPointId, prompt));
        }
        return allGeneratedCases;
    }

    private List<TestCase> parseTestCases(Long projectId, Long testPointId, String testPointName, String llmResult, String testType) {
        List<TestCase> testCases = new ArrayList<>();
        String normalizedText = stripMarkdownCodeBlock(extractLlmText(llmResult));

        try {
            JsonNode rootArray = OBJECT_MAPPER.readTree(normalizedText);
            if (rootArray.isArray()) {
                int index = 1;
                for (JsonNode node : rootArray) {
                    TestCase testCase = baseCase(projectId, testPointId, index++, testType);
                    testCase.setTitle(limitLength(node.path("title").asText(testPointName + " - case " + (index - 1)), MAX_TEST_CASE_TITLE_LENGTH));
                    testCase.setPrecondition(node.path("precondition").asText("No special precondition."));
                    testCase.setSteps(resolveSteps(node.path("steps")));
                    testCase.setExpectedResult(node.path("expectedResult").asText("The result matches expectations."));
                    testCase.setPriority(normalizePriority(node.path("priority").asText("MEDIUM")));

                    JsonNode requestDataNode = node.path("requestData");
                    if (!requestDataNode.isNull() && !requestDataNode.isMissingNode()) {
                        testCase.setRequestData(requestDataNode.toString());
                    }

                    JsonNode automationPayloadNode = node.path("automationPayload");
                    if (!automationPayloadNode.isNull() && !automationPayloadNode.isMissingNode()) {
                        testCase.setAutomationPayload(automationPayloadNode.toString());
                    }

                    normalizeTestCase(testCase);
                    testCases.add(testCase);
                }
                if (!testCases.isEmpty()) {
                    return testCases;
                }
            }
        } catch (Exception exception) {
            LLMLogger.error("TestCaseService", "parseTestCases", "Failed to parse JSON, falling back to line parsing.", exception);
        }

        int index = 1;
        for (String title : extractCandidateLines(llmResult)) {
            if (!looksLikeNoise(title)) {
                testCases.add(createCase(projectId, testPointId, index++, limitLength(title, MAX_TEST_CASE_TITLE_LENGTH), testPointName, testType));
            }
            if (testCases.size() >= 5) {
                break;
            }
        }
        return testCases;
    }

    private String generateTestCasesSafely(TestPoint testPoint, String prompt) {
        try {
            StringBuilder tpContext = new StringBuilder();
            tpContext.append("Test point name: ").append(testPoint.getName()).append("\n");
            if (testPoint.getSceneType() != null) tpContext.append("Scene type: ").append(testPoint.getSceneType()).append("\n");
            if (testPoint.getRiskLevel() != null) tpContext.append("Risk level: ").append(testPoint.getRiskLevel()).append("\n");
            if (testPoint.getBusinessRule() != null) tpContext.append("Business rule: ").append(testPoint.getBusinessRule()).append("\n");
            if (testPoint.getDescription() != null && !testPoint.getDescription().isEmpty()) {
                tpContext.append("Description: ").append(testPoint.getDescription()).append("\n");
            }
            return llmService.generateTestCases(tpContext.toString(), prompt);
        } catch (Exception exception) {
            LLMLogger.error("TestCaseService", "generateTestCasesSafely", "LLM test case generation failed.", exception);
            return testPoint.getName() == null ? "" : testPoint.getName();
        }
    }

    private Set<String> extractCandidateLines(String llmResult) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        String normalizedText = extractLlmText(llmResult);
        if (normalizedText == null || normalizedText.isBlank()) {
            return candidates;
        }
        for (String line : normalizedText.split("\\R")) {
            String candidate = normalizeCandidate(line);
            if (!candidate.isEmpty()) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            String compact = normalizeCandidate(normalizedText);
            if (!compact.isEmpty()) {
                candidates.add(compact);
            }
        }
        return candidates;
    }

    private String extractLlmText(String llmResult) {
        if (llmResult == null || llmResult.trim().isEmpty()) {
            return "";
        }
        String trimmed = llmResult.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(trimmed);
            String text = firstNonBlank(
                root.path("output").path("text").asText(null),
                root.path("output").path("message").path("content").asText(null),
                root.path("data").path("text").asText(null),
                root.path("text").asText(null),
                extractNestedText(root.path("output").path("content")),
                extractNestedText(root.path("content")),
                extractChoiceText(root.path("output").path("choices")),
                extractChoiceText(root.path("choices"))
            );
            return text == null ? trimmed : text.trim();
        } catch (Exception exception) {
            return trimmed;
        }
    }

    private String extractChoiceText(JsonNode choices) {
        if (!choices.isArray()) {
            return null;
        }
        for (JsonNode choice : choices) {
            String text = firstNonBlank(
                choice.path("text").asText(null),
                choice.path("message").path("content").asText(null),
                extractNestedText(choice.path("message").path("content")),
                extractNestedText(choice.path("content"))
            );
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String extractNestedText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : node) {
                String text = firstNonBlank(item.path("text").asText(null), item.path("content").asText(null), item.isTextual() ? item.asText() : null, extractNestedText(item));
                if (text != null && !text.isBlank()) {
                    parts.add(text.trim());
                }
            }
            return parts.isEmpty() ? null : String.join("\n", parts);
        }
        if (node.isObject()) {
            String direct = firstNonBlank(node.path("text").asText(null), node.path("content").asText(null));
            if (direct != null) {
                return direct;
            }
            var iterator = node.fields();
            while (iterator.hasNext()) {
                String nested = extractNestedText(iterator.next().getValue());
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private List<TestCase> createDefaultCases(Long projectId, Long testPointId, String testPointName, String testType) {
        List<TestCase> defaults = new ArrayList<>();
        defaults.add(createCase(projectId, testPointId, 1, testPointName + " - success path", testPointName, testType));
        defaults.add(createCase(projectId, testPointId, 2, testPointName + " - validation error", testPointName, testType));
        defaults.add(createCase(projectId, testPointId, 3, testPointName + " - boundary input", testPointName, testType));
        return defaults;
    }

    private TestCase createCase(Long projectId, Long testPointId, int index, String title, String testPointName, String testType) {
        TestCase testCase = baseCase(projectId, testPointId, index, testType);
        testCase.setTitle(title);
        testCase.setPrecondition("Project and test data are ready.");
        testCase.setSteps("1. Open the feature.\n2. Execute the scenario for " + testPointName + ".\n3. Observe the response.");
        testCase.setExpectedResult("The system handles the scenario as expected.");
        testCase.setPriority(index == 1 ? "HIGH" : "MEDIUM");
        normalizeTestCase(testCase);
        return testCase;
    }

    private TestCase baseCase(Long projectId, Long testPointId, int index, String testType) {
        TestCase testCase = new TestCase();
        testCase.setProjectId(projectId);
        testCase.setTestPointId(testPointId);
        String caseNumber = "TC-" + testPointId + "-" + index;
        testCase.setCaseNumber(caseNumber);
        testCase.setCaseCode(caseNumber);
        testCase.setCaseType(testType);
        testCase.setTestType(testType);
        testCase.setExecutionType(TestMetadata.normalizeExecutionType(null, testType));
        testCase.setStatus("PENDING_REVIEW");
        testCase.setCreator("admin");
        testCase.setCreatedBy(1L);
        return testCase;
    }

    private void normalizeTestCase(TestCase testCase) {
        if (testCase == null) {
            return;
        }
        if (testCase.getTestType() == null && testCase.getTestPointId() != null) {
            TestPoint testPoint = testPointRepository.findById(testCase.getTestPointId()).orElse(null);
            if (testPoint != null && testPoint.getTestType() != null) {
                testCase.setTestType(testPoint.getTestType());
            }
        }
        if (testCase.getTestType() == null && testCase.getRequestData() != null && !testCase.getRequestData().isBlank()) {
            testCase.setTestType(TestMetadata.TEST_TYPE_INTERFACE);
        }
        testCase.setTestType(testCase.getTestType());
        testCase.setCaseType(testCase.getCaseType() == null ? testCase.getTestType() : testCase.getCaseType());
        testCase.setExecutionType(testCase.getExecutionType());
        if (TestMetadata.TEST_TYPE_INTERFACE.equals(testCase.getTestType())
            && !TestMetadata.EXECUTION_TYPE_WEB_AUTOMATION.equals(testCase.getExecutionType())
            && (testCase.getRequestData() == null || testCase.getRequestData().isBlank())) {
            testCase.setRequestData(defaultRequestData());
        }
        if (TestMetadata.EXECUTION_TYPE_WEB_AUTOMATION.equals(testCase.getExecutionType()) && (testCase.getAutomationPayload() == null || testCase.getAutomationPayload().isBlank())) {
            testCase.setAutomationPayload(defaultAutomationPayload(testCase.getTitle()));
        }
        if (testCase.getAutomation() == null) {
            testCase.setAutomation(!TestMetadata.EXECUTION_TYPE_MANUAL.equals(testCase.getExecutionType()));
        }
    }

    private String resolveSteps(JsonNode stepsNode) {
        if (stepsNode == null || stepsNode.isNull() || stepsNode.isMissingNode()) {
            return "1. Execute the test step.";
        }
        if (stepsNode.isTextual()) {
            return stepsNode.asText("1. Execute the test step.");
        }
        if (stepsNode.isArray()) {
            List<String> lines = new ArrayList<>();
            int index = 1;
            for (JsonNode step : stepsNode) {
                String text = step.asText("");
                if (!text.isBlank()) {
                    lines.add(index++ + ". " + text.trim());
                }
            }
            if (!lines.isEmpty()) {
                return String.join("\n", lines);
            }
        }
        return "1. Execute the test step.";
    }

    private String stripMarkdownCodeBlock(String text) {
        if (text == null || !text.contains("```")) {
            return text == null ? "" : text;
        }
        String stripped = text.substring(text.indexOf("```") + 3);
        if (stripped.startsWith("json")) {
            stripped = stripped.substring(4);
        }
        if (stripped.contains("```")) {
            stripped = stripped.substring(0, stripped.lastIndexOf("```"));
        }
        return stripped.trim();
    }

    private String normalizeCandidate(String line) {
        if (line == null) {
            return "";
        }
        return line.trim()
            .replaceFirst("^#{1,6}\\s*", "")
            .replaceFirst("^[-*]+\\s*", "")
            .replaceFirst("^\\d+[.)]\\s*", "")
            .replaceAll("[\"`]", "")
            .trim();
    }

    private String normalizePriority(String priority) {
        if ("HIGH".equalsIgnoreCase(priority) || "LOW".equalsIgnoreCase(priority)) {
            return priority.toUpperCase();
        }
        return "MEDIUM";
    }

    private boolean looksLikeNoise(String candidate) {
        return candidate == null
            || candidate.startsWith("{")
            || candidate.startsWith("[")
            || candidate.startsWith("\"id\"")
            || candidate.startsWith("\"object\"")
            || candidate.startsWith("\"usage\"")
            || candidate.startsWith("request_id")
            || candidate.startsWith("output")
            || candidate.contains("\": {")
            || candidate.length() < 2;
    }

    private String limitLength(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String defaultRequestData() {
        return "{\"method\":\"POST\",\"path\":\"/api/auth/login\",\"contentType\":\"application/json\",\"body\":{\"username\":\"admin\",\"password\":\"1\"}}";
    }

    private String defaultAutomationPayload(String title) {
        String safeTitle = title == null || title.isBlank() ? "Web场景" : title.replace("\"", "'");
        return "{\"startUrl\":\"/\",\"finalUrl\":\"/\",\"finalAssertion\":\"页面打开成功\",\"steps\":[{\"action\":\"goto\",\"target\":\"/\",\"description\":\"打开首页\"},{\"action\":\"screenshot\",\"description\":\"记录页面状态\"}],\"metadata\":{\"source\":\"default\",\"title\":\"" + safeTitle + "\"}}";
    }
}
