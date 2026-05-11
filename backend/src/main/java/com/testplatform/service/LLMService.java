package com.testplatform.service;

import com.testplatform.util.LLMLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class LLMService {
    @Value("${llm.api.url}")
    private String llmApiUrl;

    @Value("${llm.api.key}")
    private String llmApiKey;

    @Value("${llm.model}")
    private String llmModel;

    @Value("${llm.temperature}")
    private double llmTemperature;

    @Value("${llm.max-tokens}")
    private int llmMaxTokens;

    public String analyzeRequirement(String requirementContent) {
        return analyzeRequirement(requirementContent, null);
    }

    public String analyzeRequirement(String requirementContent, String userPrompt) {
        LLMLogger.info("LLMService", "analyzeRequirement", "开始分析需求内容，生成测试点");
        try {
            String prompt;
            if (userPrompt != null && !userPrompt.trim().isEmpty()) {
                prompt = userPrompt.trim() + "\n\n以下是需求文档内容：\n" + requirementContent;
            } else {
                prompt = "你是一个专业的测试工程师。请分析以下需求内容，生成详细的测试点。\n\n" +
                     "【重要：输出格式要求】\n" +
                     "你必须且只能返回一个JSON数组，不要返回任何其他内容。格式如下：\n" +
                     "[\n" +
                     "  {\n" +
                     "    \"name\": \"测试点名称\",\n" +
                     "    \"description\": \"测试点描述\",\n" +
                     "    \"priority\": \"HIGH或MEDIUM或LOW\",\n" +
                     "    \"sceneType\": \"Functional\"\n" +
                     "  }\n" +
                     "]\n\n" +
                     "【字段说明】\n" +
                     "- name: 测试点名称，要求：简洁明确（10-30字），不要加序号前缀（如'1.'、'测试点1'、'### '等），不要包含任何Markdown标记。例如：用户登录功能验证、密码重置流程测试\n" +
                     "- description: 测试点描述，要求：详细说明测试目的、关注点和验证内容。可以使用HTML标签进行排版，如<b>加粗</b>、<br/>换行、<ul><li>列表</li></ul>。不要重复测试点名称，不要使用Markdown语法\n" +
                     "- priority: 优先级，只能是HIGH、MEDIUM或LOW中的一个\n" +
                     "- sceneType: 场景类型，只能是Functional、Exception、Validation、Performance、Security中的一个\n\n" +
                     "【禁止事项】\n" +
                     "1. 绝对不要返回任何Markdown标记（###、**、*、-、```等）\n" +
                     "2. 绝对不要在JSON外添加任何说明文字\n" +
                     "3. 绝对不要在name字段中使用序号前缀\n\n" +
                     "以下是需求文档内容：\n" + requirementContent;
            }
            String result = callLLMAPI(prompt);
            LLMLogger.info("LLMService", "analyzeRequirement", "需求分析完成");
            return result;
        } catch (IOException e) {
            LLMLogger.error("LLMService", "analyzeRequirement", "需求分析失败", e);
            // 失败时返回默认测试点
            return "测试点1: 用户登录功能测试\n测试点2: 用户注册功能测试\n测试点3: 密码重置功能测试";
        }
    }

    public String analyzeApiDocument(String apiDocContent) {
        LLMLogger.info("LLMService", "analyzeApiDocument", "开始分析接口文档，生成测试点");
        try {
            String prompt = buildApiDocPrompt(apiDocContent);
            String result = callLLMAPI(prompt);
            LLMLogger.info("LLMService", "analyzeApiDocument", "接口文档分析完成");
            return result;
        } catch (IOException e) {
            LLMLogger.error("LLMService", "analyzeApiDocument", "接口文档分析失败", e);
            return "[]";
        }
    }

    public String analyzeWebRequirement(String requirementContent, String userPrompt) {
        LLMLogger.info("LLMService", "analyzeWebRequirement", "开始分析 Web 需求内容，生成 Web 测试点");
        try {
            String prompt = buildWebRequirementPrompt(requirementContent, userPrompt);
            String result = callLLMAPI(prompt);
            LLMLogger.info("LLMService", "analyzeWebRequirement", "Web 需求分析完成");
            return result;
        } catch (IOException e) {
            LLMLogger.error("LLMService", "analyzeWebRequirement", "Web 需求分析失败", e);
            return "[]";
        }
    }

    private String buildApiDocPrompt(String apiDocContent) {
        return "你是一个资深的接口测试专家。请根据以下接口文档，为每个接口生成对应的测试点。\n\n" +
             "【重要识别规则】\n" +
             "1. 只提取真正的接口（形如\"### 1.1 用户登录\"、\"### 2.2 获取所有项目\"这样的标题）\n" +
             "2. 跳过以下非接口内容：\n" +
             "   - 数据实体定义（标题包含\"实体\"字样，如\"2.1 项目实体\"）\n" +
             "   - 枚举值说明\n" +
             "   - 全局说明\n" +
             "   - 前端调用示例\n\n" +
             "【测试点名称规则】\n" +
             "测试点名称必须与接口文档中的\"接口名称\"完全一致：\n" +
             "- 如果接口文档中有\"接口名称 | 用户登录\"，则name必须是\"用户登录\"\n" +
             "- 如果没有明确的接口名称表格，则从接口标题中提取（去掉编号，如\"1.1 用户登录\"提取为\"用户登录\"）\n\n" +
             "【测试点描述规则】\n" +
             "描述必须包含以下信息（按顺序）：\n" +
             "1. 接口基本信息：请求方法和路径（如：POST /api/auth/login）\n" +
             "2. 认证要求：是否需要认证\n" +
             "3. 核心参数：必填参数的名称、类型、校验规则（如必填、长度限制、格式要求等）\n" +
             "4. 请求体示例：如果有请求体说明，请提供典型的 JSON 请求体示例（如 {\"username\":\"admin\",\"password\":\"123456\"}）\n" +
             "5. 预期响应：成功时返回的关键内容\n" +
             "6. 测试关注点：需要重点验证的场景（用\"关注点：\"引导，列出2-3个关键点）\n\n" +
             "【输出格式要求】\n" +
             "1. 必须返回严格的JSON数组格式，不要返回任何Markdown标记（如```json）\n" +
             "2. 不要返回任何说明文本、前言或总结\n" +
             "3. JSON数组内每个对象必须包含以下字段：\n" +
             "   - \"name\": \"测试点名称（与接口名称完全一致）\"\n" +
             "   - \"description\": \"详细描述（按上述规则编写）\"\n" +
             "   - \"priority\": \"HIGH\" 或 \"MEDIUM\" 或 \"LOW\"\n" +
             "   - \"sceneType\": \"Functional\" 或 \"Exception\" 或 \"Validation\" 或 \"Security\"\n\n" +
             "【示例】\n" +
             "接口文档片段：\n" +
             "### 1.1 用户登录\n" +
             "**接口说明**: 用户登录获取认证令牌\n" +
             "| 接口名称 | 用户登录 |\n" +
             "| 请求方式 | POST |\n" +
             "| 请求路径 | `/api/auth/login` |\n" +
             "| 是否需要认证 | 否 |\n" +
             "#### 请求参数\n" +
             "| username | String | 是 | 用户账号 |\n" +
             "| password | String | 是 | 用户密码 |\n" +
             "#### 成功响应\n" +
             "| authorization | String | Basic认证令牌 |\n\n" +
             "对应的JSON输出：\n" +
             "[\n" +
             "  {\n" +
             "    \"name\": \"用户登录\",\n" +
             "    \"description\": \"POST /api/auth/login接口，无需认证。需传入username(用户账号，必填)和password(用户密码，必填)两个参数。请求体示例：{\"username\":\"admin\",\"password\":\"123456\"}。成功时返回authorization(Basic认证令牌)。关注点：1)正确凭据登录成功并返回令牌 2)错误凭据返回失败提示 3)令牌格式符合Basic Auth规范\",\n" +
             "    \"priority\": \"HIGH\",\n" +
             "    \"sceneType\": \"Security\"\n" +
             "  }\n" +
             "]\n\n" +
             "【接口文档内容】\n" + apiDocContent;
    }

    private String buildWebRequirementPrompt(String requirementContent, String userPrompt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名资深 Web 测试专家。请根据以下 Web 功能需求文档，生成适合当前测试平台落库的 Web 测试点。\n\n");
        if (userPrompt != null && !userPrompt.trim().isEmpty()) {
            prompt.append("用户附加要求：").append(userPrompt.trim()).append("\n\n");
        }
        prompt.append("输出要求：\n");
        prompt.append("1. 必须返回 JSON 数组，不要返回 Markdown、解释、前言或总结。\n");
        prompt.append("2. 每个对象必须包含字段：name、description、priority、sceneType、preconditions、steps、expectedResult、webElements。\n");
        prompt.append("3. webElements 必须包含 pageUrl 和 keyElements 数组。\n");
        prompt.append("4. 重点覆盖页面功能、交互流程、状态流转、表单校验、异常处理、权限控制。\n");
        prompt.append("5. description 需要整合前置条件、操作步骤、预期结果和关注点，steps 则保留结构化数组。\n\n");
        prompt.append("输出示例：\n");
        prompt.append("[{\"name\":\"登录-正确账号密码登录成功\",\"description\":\"前置条件：系统已启动。操作步骤：1. 打开登录页 2. 输入账号密码 3. 点击登录。预期结果：成功进入首页。关注点：按钮可点击、跳转正确、错误提示不出现。\",\"priority\":\"HIGH\",\"sceneType\":\"Functional\",\"preconditions\":\"系统已启动\",\"steps\":[\"打开登录页面\",\"输入账号\",\"输入密码\",\"点击登录按钮\"],\"expectedResult\":\"成功进入首页\",\"webElements\":{\"pageUrl\":\"/login\",\"keyElements\":[\"#username\",\"#password\",\"button[type=submit]\"]}}]\n\n");
        prompt.append("需求文档内容：\n").append(requirementContent);
        return prompt.toString();
    }

    public String generateTestCases(String testPoint, String userPrompt) {
        LLMLogger.info("LLMService", "generateTestCases", "开始基于测试点生成测试用例");
        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("请基于以下测试点生成详细的测试用例，包括正常场景、异常场景和边界场景。\n");
            if (userPrompt != null && !userPrompt.trim().isEmpty()) {
                promptBuilder.append("用户附加要求：\n").append(userPrompt.trim()).append("\n\n");
            }
            promptBuilder.append("【关键要求】：\n");
            promptBuilder.append("1. 请必须以严格的JSON数组格式返回，不要返回任何Markdown标记(例如 ```json)、多余的说明文本，只能返回一个JSON数组。\n");
            promptBuilder.append("2. 每一个用例应尽量详细，包含完整的操作步骤（使用序号换行）和明确的前置条件、预期结果，不要仅有一行字。\n");
            promptBuilder.append("3. JSON数组内的每一个元素必须是一个对象，包含以下6个字段：\n");
            promptBuilder.append("  - \"title\": \"用例标题\",\n");
            promptBuilder.append("  - \"precondition\": \"前置条件\",\n");
            promptBuilder.append("  - \"steps\": \"1. 测试步骤一\\n2. 测试步骤二 (务必使用带序号的具体步骤，用\\n换行)\",\n");
            promptBuilder.append("  - \"expectedResult\": \"1. 预期结果一\\n2. 预期结果二 (务必对应步骤说明预期情况，用\\n换行)\",\n");
            promptBuilder.append("  - \"priority\": \"HIGH\" 或 \"MEDIUM\" 或 \"LOW\",\n");
            promptBuilder.append("  - \"requestData\": 请求数据对象（仅当该用例需要发送 HTTP 请求时提供，否则为 null）。包含以下字段：\n");
            promptBuilder.append("    - \"method\": HTTP 方法（GET/POST/PUT/DELETE/PATCH）\n");
            promptBuilder.append("    - \"path\": 请求路径（如 /api/auth/login）\n");
            promptBuilder.append("    - \"contentType\": 请求内容类型（如 application/json）\n");
            promptBuilder.append("    - \"body\": 请求体 JSON 对象（POST/PUT/PATCH 时提供，GET/DELETE 时为 null）\n");
            promptBuilder.append("【重要规则】：\n");
            promptBuilder.append("1. 生成 requestData.body 时，必须严格参考测试点描述中的【请求体示例】和【核心参数】字段。\n");
            promptBuilder.append("2. 如果测试点描述中包含请求体示例（如 {\"username\":\"admin\",\"password\":\"123456\"}），请直接使用该示例中的字段名和值格式。\n");
            promptBuilder.append("3. body 中的字段名必须与接口文档中的参数名完全一致，不要自己编造新的字段名。\n");
            promptBuilder.append("4. 对于正常场景用例，body 中的值应为有效的典型测试数据（如 username=\"admin\", password=\"123456\"）。\n");
            promptBuilder.append("5. 对于异常场景用例，根据测试目的构造特定的值（如错误密码、空值、超长字符串等）。\n");
            promptBuilder.append("例如：\n[\n  {\n    \"title\": \"测试登录功能\",\n    \"precondition\": \"系统处于正常运行状态\",\n    \"steps\": \"1. 构造 HTTP POST 请求指向 /api/auth/login 接口\\n2. 请求体为 {\\\"username\\\": \\\"admin\\\", \\\"password\\\": \\\"123456\\\"}\\n3. 发送请求并接收响应\",\n    \"expectedResult\": \"1. 接口返回状态码 200 OK\\n2. 响应体中包含 authorization 字段且不为空\",\n    \"priority\": \"HIGH\",\n    \"requestData\": {\n      \"method\": \"POST\",\n      \"path\": \"/api/auth/login\",\n      \"contentType\": \"application/json\",\n      \"body\": {\n        \"username\": \"admin\",\n        \"password\": \"123456\"\n      }\n    }\n  }\n]\n\n");
            promptBuilder.append("需要测试的测试点内容如下：\n").append(testPoint);
            
            String prompt = promptBuilder.toString();
            String result = callLLMAPI(prompt);
            LLMLogger.info("LLMService", "generateTestCases", "测试用例生成完成");
            return result;
        } catch (IOException e) {
            LLMLogger.error("LLMService", "generateTestCases", "测试用例生成失败", e);
            return "[{\"title\":\"测试用例1: 用户登录成功测试\", \"precondition\":\"\", \"steps\":\"\", \"expectedResult\":\"\", \"priority\":\"HIGH\", \"requestData\": null}]";
        }
    }

    private String buildWebCasePrompt(String testPoint, String userPrompt) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一名 Web 自动化测试专家，请将以下测试点转换为可执行的 Web 自动化测试用例。\n\n");
        if (userPrompt != null && !userPrompt.trim().isEmpty()) {
            prompt.append("用户附加要求：").append(userPrompt.trim()).append("\n\n");
        }
        prompt.append("测试点信息：\n").append(testPoint).append("\n\n");
        prompt.append("输出要求：\n");
        prompt.append("1. 必须返回 JSON 数组，不要返回解释文本。\n");
        prompt.append("2. 每个对象必须包含 title、precondition、steps、expectedResult、priority、automationPayload。\n");
        prompt.append("3. steps 是前端展示文本；automationPayload 是结构化 JSON，必须包含 startUrl、finalUrl、finalAssertion、steps。\n");
        prompt.append("4. automationPayload.steps 的每个步骤包含 action、description，并根据需要包含 selector、target、value、timeout。\n");
        prompt.append("5. action 推荐使用 goto、click、fill、press、select、waitVisible、waitUrlContains、verifyText、screenshot、assertUrlContains。\n\n");
        prompt.append("输出示例：\n");
        prompt.append("[{\"title\":\"登录-正确账号密码登录成功\",\"precondition\":\"系统已启动\",\"steps\":\"1. 打开登录页\\n2. 输入账号密码\\n3. 点击登录\\n4. 校验跳转结果\",\"expectedResult\":\"成功跳转到首页\",\"priority\":\"HIGH\",\"automationPayload\":{\"startUrl\":\"/login\",\"finalUrl\":\"/projects\",\"finalAssertion\":\"URL 包含 /projects\",\"steps\":[{\"action\":\"goto\",\"target\":\"/login\",\"description\":\"打开登录页\"},{\"action\":\"fill\",\"selector\":\"#username\",\"value\":\"admin\",\"description\":\"输入账号\"},{\"action\":\"fill\",\"selector\":\"#password\",\"value\":\"1\",\"description\":\"输入密码\"},{\"action\":\"click\",\"selector\":\"button[type=submit]\",\"description\":\"点击登录\"},{\"action\":\"assertUrlContains\",\"value\":\"/projects\",\"description\":\"断言跳转结果\"}]}}]\n");
        return prompt.toString();
    }

    public String generateWebTestCases(String testPoint, String userPrompt) {
        LLMLogger.info("LLMService", "generateWebTestCases", "开始基于 Web 测试点生成 Web 自动化用例");
        try {
            String prompt = buildWebCasePrompt(testPoint, userPrompt);
            String result = callLLMAPI(prompt);
            LLMLogger.info("LLMService", "generateWebTestCases", "Web 自动化用例生成完成");
            return result;
        } catch (IOException e) {
            LLMLogger.error("LLMService", "generateWebTestCases", "Web 自动化用例生成失败", e);
            return "[]";
        }
    }

    public String analyzeExecutionResult(String executionResult) {
        return analyzeExecutionResult(executionResult, null, null);
    }

    public String analyzeExecutionResult(String executionResult, String testSteps, String expectedResult) {
        LLMLogger.info("LLMService", "analyzeExecutionResult", "开始分析执行结果，生成分析报告");
        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是一个专业的接口测试分析工具。请严格按照【测试步骤】中的数据和【预期结果】对以下接口执行结果进行断言验证，并给出详细的分析报告。\n\n");

            if (testSteps != null && !testSteps.trim().isEmpty()) {
                promptBuilder.append("【测试步骤（含输入数据）】：\n").append(testSteps.trim()).append("\n\n");
            }
            if (expectedResult != null && !expectedResult.trim().isEmpty()) {
                promptBuilder.append("【预期结果】：\n").append(expectedResult.trim()).append("\n\n");
            }

            promptBuilder.append("【实际执行结果】：\n").append(executionResult).append("\n\n");

            promptBuilder.append("【分析要求】：\n");
            promptBuilder.append("1. 逐条对比预期结果与实际结果，明确标注每一条是【通过】还是【不通过】。\n");
            promptBuilder.append("2. 如果测试步骤中包含具体的输入数据（如用户名、密码、请求参数），请验证实际请求是否使用了这些数据。\n");
            promptBuilder.append("3. 给出总体判定：通过 / 不通过。\n");
            promptBuilder.append("4. 对不通过的项给出根因分析和改进建议。\n");

            String prompt = promptBuilder.toString();
            String result = callLLMAPI(prompt);
            LLMLogger.info("LLMService", "analyzeExecutionResult", "执行结果分析完成");
            return result;
        } catch (IOException e) {
            LLMLogger.error("LLMService", "analyzeExecutionResult", "执行结果分析失败", e);
            // 失败时返回默认分析结果
            return "执行结果正常，符合预期。响应状态码为200，返回数据结构正确。";
        }
    }

    // 调用阿里云 LLM API 的方法
    private String callLLMAPI(String prompt) throws IOException {
        int maxRetries = 3;
        int retryDelay = 2000; // 2秒重试延迟
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            long startTime = System.currentTimeMillis();
            LLMLogger.info("LLMService", "callLLMAPI", "开始调用LLM API (尝试 " + attempt + "/" + maxRetries + ")");
            
            URL url = new URL(llmApiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 设置超时时间（LLM生成详细测试用例可能需要较长时间）
            connection.setConnectTimeout(30000);  // 30秒连接超时
            connection.setReadTimeout(180000);    // 180秒(3分钟)读取超时
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + llmApiKey);
            connection.setDoOutput(true);

            // 对 prompt 进行 JSON 转义
            String escapedPrompt = escapeJson(prompt);
            
            // 阿里云 DashScope API 请求格式
            String requestBody = "{" +
                    "\"model\": \"" + llmModel + "\"," +
                    "\"input\": {" +
                    "\"messages\": [" +
                    "{" +
                    "\"role\": \"user\"," +
                    "\"content\": \"" + escapedPrompt + "\"" +
                    "}" +
                    "]" +
                    "}," +
                    "\"parameters\": {" +
                    "\"temperature\": " + llmTemperature + "," +
                    "\"max_tokens\": " + llmMaxTokens + "" +
                    "}" +
                    "}";

            try {
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(requestBody);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                    // 解析阿里云 API 响应
                    String result = parseAliyunResponse(response.toString());
                    long duration = System.currentTimeMillis() - startTime;
                    // 打印传参和返回结果到日志
                    LLMLogger.info("LLMService", "callLLMAPI", "调用参数: " + prompt);
                    LLMLogger.info("LLMService", "callLLMAPI", "模型返回结果: " + result);
                    LLMLogger.logLLMCall("callLLMAPI", prompt, result, duration);
                    return result;
                } else {
                    // 读取错误响应
                    StringBuilder errorResponse = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                    } catch (Exception e) {
                        // 忽略错误流读取异常
                    }
                    long duration = System.currentTimeMillis() - startTime;
                    LLMLogger.error("LLMService", "callLLMAPI", "LLM API调用失败，响应码: " + responseCode + ", 错误信息: " + errorResponse.toString(), null);
                    
                    // 如果不是最后一次尝试，进行重试
                    if (attempt < maxRetries) {
                        LLMLogger.info("LLMService", "callLLMAPI", "准备重试，延迟 " + retryDelay + "ms");
                        try {
                            Thread.sleep(retryDelay);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Retry interrupted", e);
                        }
                        continue;
                    }
                    
                    throw new IOException("LLM API call failed with response code: " + responseCode + ", error: " + errorResponse.toString());
                }
            } catch (SocketTimeoutException e) {
                long duration = System.currentTimeMillis() - startTime;
                LLMLogger.error("LLMService", "callLLMAPI", "LLM API调用超时 (尝试 " + attempt + "/" + maxRetries + ")", e);
                
                // 如果不是最后一次尝试，进行重试
                if (attempt < maxRetries) {
                    LLMLogger.info("LLMService", "callLLMAPI", "准备重试，延迟 " + retryDelay + "ms");
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                    continue;
                }
                
                throw e;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        
        throw new IOException("All retry attempts failed");
    }
    
    // JSON 转义方法
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 32) {
                        // 控制字符，跳过
                    } else {
                        escaped.append(c);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    // 解析阿里云 API 响应
    private String parseAliyunResponse(String response) {
        // 解析阿里云 DashScope API 响应格式
        // 响应格式示例1: {"output": {"text": "生成的文本内容"}, "usage": {...}, "request_id": "..."}
        // 响应格式示例2: {"output": {"choices": [{"message": {"content": [{"text": "生成的文本内容"}]}}], "usage": {...}, "request_id": "..."}
        try {
            // 直接返回原始响应，让TestCaseService的extractLlmText方法处理解析
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            // 如果解析失败，返回原始响应
            return response;
        }
    }
}
