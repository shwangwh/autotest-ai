package com.testplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.testplatform.entity.Requirement;
import com.testplatform.entity.TestPoint;
import com.testplatform.repository.RequirementRepository;
import com.testplatform.repository.TestPointRepository;
import com.testplatform.util.TestMetadata;
import com.testplatform.util.LLMLogger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RequirementService {
    private static final String UPLOAD_DIR = "uploads";
    private static final int MAX_TEST_POINT_NAME_LENGTH = 200;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RequirementRepository requirementRepository;
    private final TestPointRepository testPointRepository;
    private final LLMService llmService;

    public RequirementService(
        RequirementRepository requirementRepository,
        TestPointRepository testPointRepository,
        LLMService llmService
    ) {
        this.requirementRepository = requirementRepository;
        this.testPointRepository = testPointRepository;
        this.llmService = llmService;
        ensureUploadDirectory();
    }

    public List<Requirement> getAllRequirements() {
        List<Requirement> requirements = requirementRepository.findAll();
        requirements.forEach(this::normalizeRequirement);
        return requirements;
    }

    public List<Requirement> getRequirementsByProjectId(Long projectId) {
        List<Requirement> requirements = requirementRepository.findByProjectId(projectId);
        requirements.forEach(this::normalizeRequirement);
        return requirements;
    }

    public Requirement getRequirementById(Long id) {
        Requirement requirement = requirementRepository.findById(id).orElse(null);
        return normalizeRequirement(requirement);
    }

    public Requirement uploadRequirement(Long projectId, MultipartFile file, String name, String version, String testType) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "requirement.txt" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String storedFileName = System.currentTimeMillis() + "-" + originalName;
        Path filePath = Paths.get(UPLOAD_DIR, storedFileName);
        Files.write(filePath, file.getBytes());

        Requirement requirement = new Requirement();
        requirement.setProjectId(projectId);
        requirement.setName(isBlank(name) ? originalName : name);
        requirement.setFileUrl(filePath.toString());
        if (!isBlank(version)) {
            requirement.setVersion(version);
        }
        requirement.setTestType(testType);
        return requirementRepository.save(requirement);
    }

    public List<TestPoint> generateTestPoints(Long requirementId) {
        return generateTestPoints(requirementId, null);
    }

    public List<TestPoint> generateTestPoints(Long requirementId, String userPrompt) {
        try {
            Requirement requirement = requirementRepository.findById(requirementId).orElse(null);
            if (requirement == null) {
                return List.of();
            }

            normalizeRequirement(requirement);
            String requirementContent = readRequirementContent(requirement);
            String llmResult;
            boolean interfaceRequirement = TestMetadata.TEST_TYPE_INTERFACE.equals(requirement.getTestType());
            if (interfaceRequirement) {
                LLMLogger.info("RequirementService", "generateTestPoints", "测试类型为接口测试，使用接口测试分析方法");
                llmResult = analyzeApiDocumentSafely(requirementContent);
            } else {
                LLMLogger.info("RequirementService", "generateTestPoints", "测试类型为功能测试，使用功能测试分析方法");
                llmResult = analyzeRequirementSafely(requirementContent, userPrompt);
            }

            List<TestPoint> existingPoints = testPointRepository.findByRequirementId(requirementId);
            if (!existingPoints.isEmpty()) {
                testPointRepository.deleteAll(existingPoints);
            }

            List<TestPoint> generatedPoints = parseTestPoints(
                requirement.getProjectId(),
                requirementId,
                requirement.getName(),
                llmResult,
                interfaceRequirement ? requirementContent : null,
                interfaceRequirement
                    ? TestMetadata.TEST_TYPE_INTERFACE
                    : TestMetadata.TEST_TYPE_FUNCTIONAL
            );
            if (generatedPoints.isEmpty()) {
                generatedPoints = createDefaultTestPoints(requirement.getProjectId(), requirementId, requirement.getName(), requirement.getTestType());
            }

            List<TestPoint> savedPoints = testPointRepository.saveAll(generatedPoints);
            requirement.setStatus("GENERATED");
            requirement.setTestPointCount(savedPoints.size());
            requirementRepository.save(requirement);
            return savedPoints;
        } catch (Exception e) {
            LLMLogger.error("RequirementService", "generateTestPoints", "Failed to generate or save test points", e);
            Requirement requirement = requirementRepository.findById(requirementId).orElse(null);
            if (requirement != null) {
                normalizeRequirement(requirement);
                return createDefaultTestPoints(requirement.getProjectId(), requirementId, requirement.getName(), requirement.getTestType());
            }
            return List.of();
        }
    }

    private boolean isApiDocument(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        boolean hasApiSection = content.contains("接口名称") || content.contains("请求方式") || content.contains("请求路径");
        boolean hasApiPattern = content.matches("(?s).*###\\s+\\d+\\.\\d+.*");
        boolean hasBasePath = content.contains("基础路径") || content.contains("Base URL") || content.contains("base path");
        return hasApiSection || (hasApiPattern && hasBasePath);
    }

    public void confirmTestPoints(Long requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId).orElse(null);
        if (requirement != null) {
            requirement.setStatus("CONFIRMED");
            requirementRepository.save(requirement);
        }
    }

    private void ensureUploadDirectory() {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (Files.exists(uploadPath)) {
            return;
        }

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create upload directory.", exception);
        }
    }

    private String readRequirementContent(Requirement requirement) {
        try {
            return Files.readString(Paths.get(requirement.getFileUrl()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return requirement.getName();
        }
    }

    private List<TestPoint> parseTestPoints(Long projectId, Long requirementId, String requirementName, String llmResult, String originalDoc, String testType) {
        String normalizedText = extractLlmText(llmResult);

        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }

        // 优先尝试解析JSON格式
        List<TestPoint> jsonParsedPoints = parseTestPointsFromJson(projectId, requirementId, requirementName, normalizedText, testType);
        if (!jsonParsedPoints.isEmpty()) {
            LLMLogger.info("RequirementService", "parseTestPoints", "成功从JSON格式解析出 " + jsonParsedPoints.size() + " 个测试点");
            if (originalDoc != null && !originalDoc.isBlank()) {
                validateAndFixTestPoints(jsonParsedPoints, originalDoc);
            }
            return jsonParsedPoints;
        }

        // 回退到Markdown格式解析
        LLMLogger.info("RequirementService", "parseTestPoints", "JSON解析失败，尝试Markdown格式解析");
        List<TestPoint> markdownParsedPoints = parseTestPointsFromMarkdown(projectId, requirementId, requirementName, normalizedText, testType);
        if (!markdownParsedPoints.isEmpty()) {
            LLMLogger.info("RequirementService", "parseTestPoints", "成功从Markdown格式解析出 " + markdownParsedPoints.size() + " 个测试点");
            return markdownParsedPoints;
        }

        LLMLogger.warn("RequirementService", "parseTestPoints", "所有格式解析失败，返回空列表");
        return List.of();
    }

    private List<TestPoint> parseTestPoints(Long projectId, Long requirementId, String requirementName, String llmResult) {
        return parseTestPoints(projectId, requirementId, requirementName, llmResult, null, TestMetadata.TEST_TYPE_INTERFACE);
    }

    private List<TestPoint> parseTestPointsFromJson(Long projectId, Long requirementId, String requirementName, String llmResult, String testType) {
        List<TestPoint> testPoints = new ArrayList<>();
        try {
            String trimmed = llmResult.trim();

            // 去除可能的markdown代码块标记
            if (trimmed.startsWith("```")) {
                trimmed = trimmed.replaceAll("(?s)^```\\w*\\n?", "").replaceAll("\\n?```\\s*$", "").trim();
            }

            if (!trimmed.startsWith("[")) {
                return testPoints;
            }

            JsonNode jsonArray = OBJECT_MAPPER.readTree(trimmed);
            if (!jsonArray.isArray()) {
                return testPoints;
            }

            int index = 1;
            for (JsonNode node : jsonArray) {
                if (testPoints.size() >= 50) {
                    break;
                }

                String name = cleanMarkdownName(node.path("name").asText(null));
                String description = buildPointDescription(node.path("description").asText(null), node);
                String priority = node.path("priority").asText("MEDIUM");
                String sceneType = node.path("sceneType").asText("Functional");

                if (name == null || name.isBlank()) {
                    continue;
                }

                TestPoint point = new TestPoint();
                point.setProjectId(projectId);
                point.setRequirementId(requirementId);
                point.setRequirementItemId(null);
                point.setPointCode(limitLength("TP-" + requirementId + "-" + index, 50));
                point.setName(limitLength(name, MAX_TEST_POINT_NAME_LENGTH));
                point.setPointName(limitLength(name, MAX_TEST_POINT_NAME_LENGTH));
                point.setPointType(TestMetadata.normalizeTestType(testType));
                point.setTestType(testType);
                point.setSourceType(TestMetadata.POINT_SOURCE_LLM_GENERATED);
                point.setFunction(limitLength(requirementName, 200));
                point.setSceneType(sceneType);
                point.setRiskLevel(normalizePriority(priority));
                point.setBusinessRule("Generated from uploaded requirement.");
                point.setAutomationSuggested(true);
                point.setDescription(description != null && !description.isBlank() ? description.trim() : name);

                testPoints.add(point);
                index++;
            }
        } catch (Exception e) {
            LLMLogger.error("RequirementService", "parseTestPointsFromJson", "JSON解析失败", e);
        }
        return testPoints;
    }

    private String cleanMarkdownName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }
        String name = rawName.trim();

        // 去除Markdown标题标记
        name = name.replaceFirst("^#{1,6}\\s*", "");
        // 去除粗体标记
        name = name.replaceAll("\\*\\*", "");
        name = name.replaceAll("__", "");
        // 去除斜体标记
        name = name.replaceAll("\\*", "");
        name = name.replaceAll("_", "");
        // 去除代码标记
        name = name.replaceAll("`", "");
        // 去除序号前缀
        name = name.replaceFirst("^\\d+[.)、]\\s*", "");
        name = name.replaceFirst("^第[一二三四五六七八九十]+[条章节]\\s*", "");
        // 去除"测试点"前缀
        name = name.replaceFirst("^测试点[:：]?\\s*", "");
        name = name.replaceFirst("^用例[:：]?\\s*", "");
        // 去除链接标记
        name = name.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1");
        // 去除HTML标签（名称中不应有HTML）
        name = name.replaceAll("<[^>]*>", "");

        return name.trim();
    }

    private List<TestPoint> parseTestPointsFromMarkdown(Long projectId, Long requirementId, String requirementName, String normalizedText, String testType) {
        List<TestPoint> testPoints = new ArrayList<>();

        // Match common block headings like "### 标题" or "1. 标题" or "- **标题**"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?m)^(#{1,6}\\s+.*|\\d+\\.\\s+.*|[-*]\\s+\\*{2}.*\\*{2}.*)$");
        java.util.regex.Matcher matcher = pattern.matcher(normalizedText);

        int lastEnd = 0;
        String currentTitle = null;

        while (matcher.find()) {
            if (currentTitle != null) {
                String desc = normalizedText.substring(lastEnd, matcher.start()).trim();
                addTestPointChunk(testPoints, projectId, requirementId, requirementName, currentTitle, desc, testType);
            }
            currentTitle = matcher.group(1).trim();
            lastEnd = matcher.end();
            if (testPoints.size() >= 50) {
                return testPoints;
            }
        }

        if (currentTitle != null) {
            String desc = normalizedText.substring(lastEnd).trim();
            addTestPointChunk(testPoints, projectId, requirementId, requirementName, currentTitle, desc, testType);
        } else {
            // No recognizable markdown headings/lists found.
            // Treat the whole output as a single block
            String desc = normalizedText.trim();
            String name = limitLength(requirementName, MAX_TEST_POINT_NAME_LENGTH);
            String[] lines = desc.split("\\R");
            for (String line : lines) {
                if (!line.trim().isEmpty() && !looksLikeNoise(line.trim())) {
                    name = line.trim();
                    break;
                }
            }
            addTestPointChunk(testPoints, projectId, requirementId, requirementName, name, desc, testType);
        }

        return testPoints;
    }

    private String normalizePriority(String priority) {
        if (priority == null) {
            return "MEDIUM";
        }
        String upper = priority.toUpperCase().trim();
        switch (upper) {
            case "HIGH":
            case "P0":
            case "P1":
                return "HIGH";
            case "MEDIUM":
            case "MIDDLE":
            case "P2":
                return "MEDIUM";
            case "LOW":
            case "P3":
                return "LOW";
            default:
                return "MEDIUM";
        }
    }

    private String buildPointDescription(String description, JsonNode node) {
        if (description != null && !description.isBlank()) {
            return description;
        }

        List<String> sections = new ArrayList<>();
        String preconditions = node.path("preconditions").asText(null);
        if (preconditions != null && !preconditions.isBlank()) {
            sections.add("前置条件：" + preconditions.trim());
        }

        JsonNode stepsNode = node.path("steps");
        if (stepsNode.isArray() && !stepsNode.isEmpty()) {
            List<String> stepTexts = new ArrayList<>();
            int index = 1;
            for (JsonNode step : stepsNode) {
                String text = step.asText("");
                if (!text.isBlank()) {
                    stepTexts.add(index++ + ". " + text.trim());
                }
            }
            if (!stepTexts.isEmpty()) {
                sections.add("操作步骤：\n" + String.join("\n", stepTexts));
            }
        }

        String expectedResult = node.path("expectedResult").asText(null);
        if (expectedResult != null && !expectedResult.isBlank()) {
            sections.add("预期结果：" + expectedResult.trim());
        }

        JsonNode webElementsNode = node.path("webElements");
        if (webElementsNode.isObject()) {
            List<String> elementParts = new ArrayList<>();
            String pageUrl = webElementsNode.path("pageUrl").asText(null);
            if (pageUrl != null && !pageUrl.isBlank()) {
                elementParts.add("页面：" + pageUrl.trim());
            }
            JsonNode keyElements = webElementsNode.path("keyElements");
            if (keyElements.isArray() && !keyElements.isEmpty()) {
                List<String> elements = new ArrayList<>();
                for (JsonNode keyElement : keyElements) {
                    String text = keyElement.asText("");
                    if (!text.isBlank()) {
                        elements.add(text.trim());
                    }
                }
                if (!elements.isEmpty()) {
                    elementParts.add("关键元素：" + String.join("、", elements));
                }
            }
            if (!elementParts.isEmpty()) {
                sections.add(String.join("；", elementParts));
            }
        }

        return sections.isEmpty() ? null : String.join("\n\n", sections);
    }

    private void addTestPointChunk(List<TestPoint> testPoints, Long projectId, Long requirementId, String requirementName, String title, String desc, String testType) {
        String cleanTitle = normalizeCandidate(title);
        if (cleanTitle.isEmpty() || looksLikeNoise(cleanTitle)) {
            cleanTitle = "Test Point " + (testPoints.size() + 1);
        }

        TestPoint point = new TestPoint();
        int index = testPoints.size() + 1;
        point.setProjectId(projectId);
        point.setRequirementId(requirementId);
        point.setRequirementItemId(null);
        point.setPointCode(limitLength("TP-" + requirementId + "-" + index, 50));
        point.setName(limitLength(cleanTitle, MAX_TEST_POINT_NAME_LENGTH));
        point.setPointName(limitLength(cleanTitle, MAX_TEST_POINT_NAME_LENGTH));
        point.setPointType(TestMetadata.normalizeTestType(testType));
        point.setTestType(testType);
        point.setSourceType(TestMetadata.POINT_SOURCE_LLM_GENERATED);
        point.setFunction(limitLength(requirementName, 200));
        point.setSceneType(TestMetadata.TEST_TYPE_INTERFACE.equals(testType) ? "Validation" : "Functional");
        point.setRiskLevel("MEDIUM");
        point.setBusinessRule("Generated from uploaded requirement.");
        point.setAutomationSuggested(true);
        
        String fullDescription = title + "\n\n" + desc;
        point.setDescription(fullDescription.trim());
        
        testPoints.add(point);
    }

    private String analyzeRequirementSafely(String requirementContent, String userPrompt) {
        LLMLogger.info("RequirementService", "analyzeRequirementSafely", "开始调用LLM服务分析需求");
        if (userPrompt != null && !userPrompt.isBlank()) {
            LLMLogger.info("RequirementService", "analyzeRequirementSafely", "用户自定义提示语: " + userPrompt);
        }
        try {
            String result = llmService.analyzeRequirement(requirementContent, userPrompt);
            LLMLogger.info("RequirementService", "analyzeRequirementSafely", "LLM服务分析需求完成");
            return result;
        } catch (Exception exception) {
            LLMLogger.error("RequirementService", "analyzeRequirementSafely", "LLM服务分析需求失败", exception);
            return requirementContent == null ? "" : requirementContent;
        }
    }

    private String analyzeApiDocumentSafely(String apiDocContent) {
        LLMLogger.info("RequirementService", "analyzeApiDocumentSafely", "开始调用LLM服务分析接口文档");
        try {
            String result = llmService.analyzeApiDocument(apiDocContent);
            LLMLogger.info("RequirementService", "analyzeApiDocumentSafely", "LLM服务分析接口文档完成");
            return result;
        } catch (Exception exception) {
            LLMLogger.error("RequirementService", "analyzeApiDocumentSafely", "LLM服务分析接口文档失败", exception);
            return "[]";
        }
    }

    private String analyzeWebRequirementSafely(String requirementContent, String userPrompt) {
        LLMLogger.info("RequirementService", "analyzeWebRequirementSafely", "开始调用LLM服务分析Web需求");
        try {
            String result = llmService.analyzeWebRequirement(requirementContent, userPrompt);
            LLMLogger.info("RequirementService", "analyzeWebRequirementSafely", "LLM服务分析Web需求完成");
            return result;
        } catch (Exception exception) {
            LLMLogger.error("RequirementService", "analyzeWebRequirementSafely", "LLM服务分析Web需求失败", exception);
            return "[]";
        }
    }

    private Map<String, String> extractInterfaceNames(String docContent) {
        Map<String, String> interfaceMap = new HashMap<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "###\\s+\\d+\\.\\d+\\s+(.+?)\\n[\\s\\S]*?\\|\\s*接口名称\\s*\\|\\s*(.+?)\\s*\\|"
        );
        java.util.regex.Matcher matcher = pattern.matcher(docContent);
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            String interfaceName = matcher.group(2).trim();
            interfaceMap.put(title, interfaceName);
        }
        return interfaceMap;
    }

    private void validateAndFixTestPoints(List<TestPoint> testPoints, String originalDoc) {
        Map<String, String> interfaceNames = extractInterfaceNames(originalDoc);
        Set<String> validNames = new HashSet<>(interfaceNames.values());
        if (validNames.isEmpty()) {
            return;
        }
        for (TestPoint point : testPoints) {
            String name = point.getPointName();
            if (!validNames.contains(name)) {
                LLMLogger.warn("RequirementService", "validateAndFixTestPoints",
                    "测试点名称 '" + name + "' 与接口名称不匹配");
                String correctedName = findClosestInterface(name, validNames);
                if (correctedName != null) {
                    LLMLogger.info("RequirementService", "validateAndFixTestPoints",
                        "修正测试点名称: '" + name + "' -> '" + correctedName + "'");
                    point.setPointName(correctedName);
                    point.setName(correctedName);
                }
            }
        }
    }

    private String findClosestInterface(String name, Set<String> validNames) {
        for (String validName : validNames) {
            if (validName.contains(name) || name.contains(validName)) {
                return validName;
            }
        }
        return null;
    }

    // extractCandidateLines has been replaced by block-based extraction in parseTestPoints

    private String extractLlmText(String llmResult) {
        if (llmResult == null) {
            return "";
        }

        String trimmed = llmResult.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return trimmed;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(trimmed);
            
            // 如果根节点是JSON数组，直接返回原始JSON（用于测试点解析）
            if (root.isArray()) {
                return trimmed;
            }

            // 如果是JSON对象，尝试从中提取文本内容
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
                String text = firstNonBlank(
                    item.path("text").asText(null),
                    item.path("content").asText(null),
                    extractNestedText(item)
                );
                if (text != null && !text.isBlank()) {
                    parts.add(text.trim());
                }
            }
            return parts.isEmpty() ? null : String.join("\n", parts);
        }

        if (node.isObject()) {
            String direct = firstNonBlank(
                node.path("text").asText(null),
                node.path("content").asText(null)
            );
            if (direct != null) {
                return direct;
            }

            var iterator = node.fields();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                String nested = extractNestedText(entry.getValue());
                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }

    private String normalizeCandidate(String line) {
        if (line == null) {
            return "";
        }

        String candidate = line.trim();
        if (candidate.isEmpty()) {
            return "";
        }

        candidate = candidate
            .replaceFirst("^#{1,6}\\s*", "") // 去除Markdown标题标记
            .replaceFirst("^[-*•]+\\s*", "")
            .replaceFirst("^\\d+[.)、\\s]+", "")
            .replaceFirst("^测试点[:：]\\s*", "")
            .replaceFirst("^用例[:：]\\s*", "")
            .replaceAll("[\"`]", "")
            .trim();

        return candidate;
    }

    private boolean looksLikeNoise(String candidate) {
        return candidate.startsWith("{")
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

    private List<TestPoint> createDefaultTestPoints(Long projectId, Long requirementId, String requirementName, String testType) {
        List<TestPoint> defaults = new ArrayList<>();
        defaults.add(createTestPoint(projectId, requirementId, 1, requirementName, "Happy path validation", "Happy path", "HIGH", testType));
        defaults.add(createTestPoint(projectId, requirementId, 2, requirementName, "Input validation", "Validation", "MEDIUM", testType));
        defaults.add(createTestPoint(projectId, requirementId, 3, requirementName, "Permission and exception handling", "Exception", "HIGH", testType));
        return defaults;
    }

    private TestPoint createTestPoint(Long projectId, Long requirementId, int index, String requirementName, String name, String sceneType, String riskLevel, String testType) {
        TestPoint point = new TestPoint();
        point.setProjectId(projectId);
        point.setRequirementId(requirementId);
        point.setRequirementItemId(null);
        point.setPointCode("TP-" + requirementId + "-" + index);
        point.setName(name);
        point.setPointName(name);
        point.setPointType(TestMetadata.normalizeTestType(testType));
        point.setTestType(testType);
        point.setSourceType(TestMetadata.POINT_SOURCE_MIGRATED);
        point.setFunction(requirementName);
        point.setSceneType(sceneType);
        point.setRiskLevel(riskLevel);
        point.setBusinessRule("Default rule generated by the system.");
        point.setAutomationSuggested(true);
        return point;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Requirement normalizeRequirement(Requirement requirement) {
        if (requirement == null) {
            return null;
        }
        requirement.setTestType(requirement.getTestType());
        return requirement;
    }
}
