# LLM提取接口测试点优化方案

## 一、问题分析

### 当前问题
1. **测试点名称不准确**：LLM生成的名称与接口文档中的接口名称不对应
2. **描述信息不充分**：没有充分利用接口定义、字段定义、响应示例等详情
3. **包含噪音数据**：将实体定义、枚举值等非接口内容也当作测试点
4. **输出格式不稳定**：有时返回Markdown表格，有时返回JSON，解析困难

### 根因分析
- Prompt过于通用，没有针对接口文档结构优化
- 没有明确指定测试点名称的来源（接口名称字段）
- 没有指导LLM如何提取和利用接口详情信息

---

## 二、优化方案

### 2.1 接口文档结构分析

```
## 一、认证模块 (Auth)          <- 模块标题
**基础路径**: `/api/auth`       <- 模块基础路径

### 1.1 用户登录                 <- 接口标题（需要提取）
**接口说明**: 用户登录获取认证令牌  <- 接口说明

| 项目 | 说明 |                  <- 接口定义表格
|------|------|
| 接口名称 | 用户登录 |        <- 【测试点名称来源】
| 请求方式 | POST |            <- 请求方法
| 请求路径 | `/api/auth/login` | <- 接口路径
| 是否需要认证 | 否 |           <- 认证要求

#### 请求参数                     <- 请求参数表格
| 参数名 | 类型 | 必填 | 说明 |
| username | String | 是 | 用户账号 |
| password | String | 是 | 用户密码 |

#### 成功响应                     <- 响应参数表格
| 参数名 | 类型 | 说明 |
| authorization | String | Basic认证令牌 |

#### 成功响应示例                 <- 响应示例
{ "success": true, "authorization": "Basic ..." }
```

### 2.2 核心设计原则

| 原则 | 说明 |
|------|------|
| **名称对应** | 测试点名称 = 接口文档中的"接口名称"字段值 |
| **描述完整** | 描述需包含：接口路径、请求方法、认证要求、核心参数、预期响应、测试关注点 |
| **过滤噪音** | 跳过实体定义（包含"实体"字样）、枚举值、全局说明等非接口内容 |
| **格式稳定** | 强制JSON数组输出，提供明确的示例模板 |

---

## 三、Prompt设计

### 3.1 主Prompt模板

```
你是一个资深的接口测试专家。请根据以下接口文档，为每个接口生成对应的测试点。

【重要识别规则】
1. 只提取真正的接口（形如"### 1.1 用户登录"、"### 2.2 获取所有项目"这样的标题）
2. 跳过以下非接口内容：
   - 数据实体定义（标题包含"实体"字样，如"2.1 项目实体"）
   - 枚举值说明
   - 全局说明
   - 前端调用示例

【测试点名称规则】
测试点名称必须与接口文档中的"接口名称"完全一致：
- 如果接口文档中有"接口名称 | 用户登录"，则name必须是"用户登录"
- 如果没有明确的接口名称表格，则从接口标题中提取（去掉编号，如"1.1 用户登录"提取为"用户登录"）

【测试点描述规则】
描述必须包含以下信息（按顺序）：
1. 接口基本信息：请求方法和路径（如：POST /api/auth/login）
2. 认证要求：是否需要认证
3. 核心参数：必填参数的名称和作用
4. 预期响应：成功时返回的关键内容
5. 测试关注点：需要重点验证的场景（用"关注点："引导，列出2-3个关键点）

【输出格式要求】
1. 必须返回严格的JSON数组格式，不要返回任何Markdown标记（如```json）
2. 不要返回任何说明文本、前言或总结
3. JSON数组内每个对象必须包含以下字段：
   - "name": "测试点名称（与接口名称完全一致）"
   - "description": "详细描述（按上述规则编写）"
   - "priority": "HIGH" 或 "MEDIUM" 或 "LOW"
   - "sceneType": "Functional" 或 "Exception" 或 "Validation" 或 "Security"

【示例】
接口文档片段：
### 1.1 用户登录
**接口说明**: 用户登录获取认证令牌
| 接口名称 | 用户登录 |
| 请求方式 | POST |
| 请求路径 | `/api/auth/login` |
| 是否需要认证 | 否 |
#### 请求参数
| username | String | 是 | 用户账号 |
| password | String | 是 | 用户密码 |
#### 成功响应
| authorization | String | Basic认证令牌 |

对应的JSON输出：
[
  {
    "name": "用户登录",
    "description": "POST /api/auth/login接口，无需认证。需传入username(用户账号)和password(用户密码)两个必填参数。成功时返回authorization(Basic认证令牌)。关注点：1)正确凭据登录成功并返回令牌 2)错误凭据返回失败提示 3)令牌格式符合Basic Auth规范",
    "priority": "HIGH",
    "sceneType": "Functional"
  }
]

【接口文档内容】
{requirementContent}
```

### 3.2 优先级判定规则

| 优先级 | 判定条件 |
|--------|----------|
| HIGH | 核心业务接口（登录、创建、支付等）、涉及资金/安全、用户高频使用 |
| MEDIUM | 常规CRUD操作、查询类接口、辅助功能 |
| LOW | 删除接口（非核心数据）、配置类接口、统计报表 |

### 3.3 场景类型判定规则

| 场景类型 | 适用接口 |
|----------|----------|
| Functional | 正常的业务功能接口（创建、查询、更新等） |
| Exception | 异常处理、错误场景验证 |
| Validation | 参数校验、格式验证接口 |
| Security | 登录认证、权限验证、敏感操作 |
| Performance | 列表查询、批量操作、大数据量接口 |

---

## 四、代码实现优化

### 4.1 LLMService优化

```java
// 新增：针对接口文档的专用分析方法
public String analyzeApiDocument(String apiDocContent) {
    String prompt = buildApiDocPrompt(apiDocContent);
    return callLLMAPI(prompt);
}

// 构建接口文档专用Prompt
private String buildApiDocPrompt(String apiDocContent) {
    return """
        你是一个资深的接口测试专家。请根据以下接口文档，为每个接口生成对应的测试点。
        
        【重要识别规则】
        1. 只提取真正的接口（形如"### 1.1 xxx"、"### 2.2 xxx"这样的标题）
        2. 跳过以下非接口内容：
           - 数据实体定义（标题包含"实体"字样）
           - 枚举值说明
           - 全局说明
           - 前端调用示例
        
        【测试点名称规则】
        测试点名称必须与接口文档中的"接口名称"完全一致。
        如果没有明确的"接口名称"字段，则从接口标题中提取（去掉编号前缀）。
        
        【测试点描述规则】
        描述必须包含：
        1. 接口基本信息：请求方法和路径
        2. 认证要求
        3. 核心参数说明
        4. 预期响应内容
        5. 测试关注点（2-3个关键验证场景）
        
        【输出格式要求】
        1. 必须返回严格的JSON数组格式
        2. 不要返回任何Markdown标记、说明文本
        3. 每个对象包含：name, description, priority, sceneType
        
        【接口文档内容】
        """ + apiDocContent;
}
```

### 4.2 RequirementService优化

```java
// 优化parseTestPoints方法
private List<TestPoint> parseTestPointsFromApiDoc(
    Long projectId, 
    Long requirementId, 
    String requirementName,
    String llmResult
) {
    List<TestPoint> testPoints = new ArrayList<>();
    
    // 1. 尝试JSON解析
    try {
        JSONArray jsonArray = new JSONArray(llmResult);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            TestPoint point = new TestPoint();
            point.setProjectId(projectId);
            point.setRequirementId(requirementId);
            
            // 直接使用LLM返回的name（已要求与接口名称对应）
            String name = obj.getString("name");
            point.setPointName(limitLength(name.trim(), MAX_TEST_POINT_NAME_LENGTH));
            point.setName(name.trim());
            
            // 使用结构化的description
            String description = obj.getString("description");
            point.setDescription(description.trim());
            
            // 设置其他字段
            point.setSceneType(normalizeSceneType(obj.optString("sceneType", "Functional")));
            point.setRiskLevel(normalizePriority(obj.optString("priority", "MEDIUM")));
            point.setPointType("API");
            point.setAutomationSuggested(true);
            
            testPoints.add(point);
        }
    } catch (Exception e) {
        // JSON解析失败，回退到Markdown解析
        testPoints = parseTestPointsFromMarkdown(llmResult, projectId, requirementId);
    }
    
    return testPoints;
}

// 从原文档中提取接口名称映射（用于验证）
private Map<String, String> extractInterfaceNames(String docContent) {
    Map<String, String> interfaceMap = new HashMap<>();
    
    // 使用正则提取接口名称
    Pattern pattern = Pattern.compile(
        "###\\s+\\d+\\.\\d+\\s+(.+?)\\n[\\s\\S]*?\\|\\s*接口名称\\s*\\|\\s*(.+?)\\s*\\|"
    );
    Matcher matcher = pattern.matcher(docContent);
    
    while (matcher.find()) {
        String title = matcher.group(1).trim();
        String interfaceName = matcher.group(2).trim();
        interfaceMap.put(title, interfaceName);
    }
    
    return interfaceMap;
}
```

### 4.3 验证和修正逻辑

```java
// 验证测试点名称是否与接口对应
private void validateAndFixTestPoints(
    List<TestPoint> testPoints, 
    String originalDoc
) {
    Map<String, String> interfaceNames = extractInterfaceNames(originalDoc);
    Set<String> validNames = new HashSet<>(interfaceNames.values());
    
    for (TestPoint point : testPoints) {
        String name = point.getPointName();
        
        // 如果名称不在接口名称列表中，记录警告
        if (!validNames.contains(name)) {
            LLMLogger.warn("RequirementService", "validateAndFixTestPoints", 
                "测试点名称 '" + name + "' 与接口名称不匹配");
            
            // 可选：尝试自动修正
            String correctedName = findClosestInterface(name, validNames);
            if (correctedName != null) {
                point.setPointName(correctedName);
                point.setName(correctedName);
            }
        }
    }
}

// 查找最接近的接口名称（简单的字符串匹配）
private String findClosestInterface(String name, Set<String> validNames) {
    // 可以使用编辑距离或简单的包含匹配
    for (String validName : validNames) {
        if (validName.contains(name) || name.contains(validName)) {
            return validName;
        }
    }
    return null;
}
```

---

## 五、预期效果

### 5.1 提取结果对比

| 项目 | 优化前 | 优化后 |
|------|--------|--------|
| 测试点名称 | 自由生成，如"验证使用正确的用户名和密码能否成功登录" | 固定为接口名称，如"用户登录" |
| 描述内容 | 简单或不完整 | 结构化：路径+方法+参数+响应+关注点 |
| 噪音过滤 | 可能包含实体定义 | 明确跳过非接口内容 |
| 输出稳定性 | JSON/Markdown混合 | 严格JSON数组 |
| 解析成功率 | ~60% | ~95%+ |

### 5.2 示例输出

**输入**：接口文档中的"用户登录"接口

**优化后输出**：
```json
[
  {
    "name": "用户登录",
    "description": "POST /api/auth/login接口，无需认证。需传入username(用户账号)和password(用户密码)两个必填参数。成功时返回authorization(Basic认证令牌)和username。关注点：1)正确凭据登录成功并返回有效令牌 2)错误凭据返回失败提示 3)令牌格式符合Basic Base64规范",
    "priority": "HIGH",
    "sceneType": "Security"
  }
]
```

---

## 六、实施步骤

1. **修改LLMService.java**
   - 添加`analyzeApiDocument()`方法
   - 优化Prompt模板

2. **修改RequirementService.java**
   - 优化`parseTestPoints()`方法
   - 添加接口名称提取和验证逻辑
   - 改进JSON解析逻辑

3. **测试验证**
   - 使用现有接口文档测试
   - 对比优化前后效果
   - 验证测试点名称与接口对应关系

4. **配置调优**
   - 根据测试结果调整Prompt
   - 优化LLM参数（temperature等）

---

## 七、注意事项

1. **LLM模型选择**：建议使用较强的模型（如qwen-max、gpt-4等）以确保JSON输出稳定性
2. **Temperature设置**：建议设置为0.1-0.3，降低随机性，提高输出稳定性
3. **超时设置**：接口文档较长时，适当增加超时时间
4. **重试机制**：JSON解析失败时，可以尝试让LLM重新生成
5. **回退策略**：如果LLM输出始终不理想，可以回退到基于规则的解析方式
