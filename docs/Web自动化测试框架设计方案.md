# Web 自动化测试框架设计方案

## 1. 整体架构

### 1.1 端到端流程

```
┌─────────────────────────────────────────────────────────────────┐
│ 阶段1：Web 功能需求 → 测试点提取                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  功能需求文档 (Markdown)                                         │
│  示例：智能测试平台_V1需求文档.md                                  │
│        ↓                                                        │
│  [LLM Web 测试点提取引擎]                                         │
│  - 识别功能模块和交互场景                                          │
│  - 提取页面功能、操作流程、状态流转                                │
│  - 过滤技术细节、未来规划                                          │
│  - 返回包含页面元素信息的测试点                                    │
│        ↓                                                        │
│  Web 测试点列表 (JSON)                                            │
│  [{                                                             │
│    "name": "登录 - 使用正确凭据登录成功",                         │
│    "preconditions": "系统已启动",                                 │
│    "steps": ["打开登录页面", "输入账号", ...],                   │
│    "expectedResult": "进入项目列表页",                           │
│    "webElements": {                                              │
│      "pageUrl": "/login",                                        │
│      "keyElements": ["#username", "#password", "text=登录"]      │
│    }                                                             │
│  }]                                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────────┐
│ 阶段2：Web 测试点 → Playwright 自动化用例转换                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Web 测试点列表                                                  │
│        ↓                                                        │
│  [LLM 自动化用例转换器]                                           │
│  - 分析测试点的步骤和页面元素                                      │
│  - 结合目标系统实际页面结构                                        │
│  - 生成 Playwright 操作序列                                      │
│        ↓                                                        │
│  Playwright 测试用例 (JSON)                                      │
│  [{                                                             │
│    "test_case_name": "登录 - 使用正确凭据登录成功",              │
│    "steps": [                                                   │
│      {"action": "goto", "url": "http://localhost:3000/login"},  │
│      {"action": "fill", "selector": "#username", "value": "admin"}, │
│      {"action": "fill", "selector": "#password", "value": "1"}, │
│      {"action": "click", "selector": "text=登录"},               │
│      {"action": "assert", "expected_url_contains": "/projects"} │
│    ]                                                            │
│  }]                                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
        ↓
┌─────────────────────────────────────────────────────────────────┐
│ 阶段3：Playwright 执行 + Allure 报告                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  自动化测试用例                                                   │
│        ↓                                                        │
│  [Playwright MCP 执行引擎]                                       │
│  - 启动浏览器，执行操作                                           │
│  - 每步截图 + 记录结果                                           │
│        ↓                                                        │
│  [Allure 报告生成器]                                             │
│  - 生成可视化报告（含截图）                                       │
│        ↓                                                        │
│  📊 Allure 报告                                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 2. 核心模块设计

### 2.1 测试点提取模块 (extractors/web_test_point_extractor.py)

```python
class WebTestPointExtractor:
    """从 Web 功能需求文档提取测试点"""
    
    def extract_from_requirement_doc(self, doc_content: str) -> List[WebTestPoint]:
        """
        从功能需求文档提取测试点
        
        Prompt 设计：
        - 识别功能模块和交互场景
        - 提取页面功能、操作流程、状态流转
        - 过滤技术细节、未来规划
        - 返回包含页面元素信息的测试点
        """
        prompt = self._build_web_requirement_prompt(doc_content)
        llm_response = self.llm_client.call(prompt)
        return self._parse_web_test_points(llm_response)
    
    def _build_web_requirement_prompt(self, doc_content: str) -> str:
        """构建针对 Web 功能需求的 Prompt"""
        return f"""
        你是一个资深的 Web 功能测试专家。请根据以下功能需求文档，为每个功能模块和交互场景生成对应的测试点。

        【重要识别规则】
        1. 识别真正的功能测试场景（如"用户登录"、"创建项目"、"上传需求文件"）
        2. 跳过以下内容：
           - 技术实现细节（数据库结构、API 路径）
           - 部署/运维相关说明
           - 性能指标建议（除非明确是性能测试需求）
           - 未来规划（如"二期规划"、"后续扩展"）

        【测试点提取维度】
        从以下维度提取测试点：
        1. **页面功能**：每个页面的核心功能（如登录页的账号密码输入、登录按钮）
        2. **交互流程**：多步骤业务流程（如：上传需求 → 生成测试点 → 确认测试点）
        3. **状态流转**：状态变化场景（如：需求状态从"待分析" → "分析中" → "已分析"）
        4. **权限控制**：不同角色的操作权限（如：管理员 vs 测试人员）
        5. **边界场景**：异常处理、空数据、极限值
        6. **数据验证**：输入校验、格式验证、必填项检查

        【测试点名称规则】
        测试点名称 = 功能模块 + 操作/场景描述
        示例：
        - "登录 - 使用正确凭据登录成功"
        - "项目管理 - 创建新项目"
        - "需求设计 - 上传需求文件并生成测试点"

        【测试点描述规则】
        描述必须包含以下信息（按顺序）：
        1. 前置条件：执行该功能前需要满足的条件
        2. 操作步骤：用户需要执行的关键操作（2-5步）
        3. 预期结果：操作成功后应该看到的结果
        4. 测试关注点：需要重点验证的场景（用"关注点："引导，列出2-3个关键点）

        【优先级判定规则】
        | 优先级 | 判定条件 |
        |--------|----------|
        | HIGH | 核心业务流程（登录、创建、主流程）、涉及资金/安全、用户高频使用 |
        | MEDIUM | 常规CRUD操作、查询类功能、辅助功能 |
        | LOW | 删除功能（非核心数据）、配置类功能、统计展示 |

        【场景类型判定规则】
        | 场景类型 | 适用场景 |
        |----------|----------|
        | Functional | 正常的业务功能操作（创建、查询、编辑等） |
        | Exception | 异常处理、错误场景验证 |
        | Validation | 输入校验、格式验证、必填项检查 |
        | Security | 登录认证、权限验证、敏感操作 |
        | Workflow | 多步骤流程、状态流转场景 |

        【输出格式要求】
        1. 必须返回严格的JSON数组格式，不要返回任何Markdown标记（如```json）
        2. 不要返回任何说明文本、前言或总结
        3. JSON数组内每个对象必须包含以下字段：
           - "name": "测试点名称"
           - "description": "详细描述（按上述规则编写）"
           - "priority": "HIGH" 或 "MEDIUM" 或 "LOW"
           - "sceneType": "Functional" 或 "Exception" 或 "Validation" 或 "Security" 或 "Workflow"
           - "preconditions": "前置条件描述"
           - "steps": ["操作步骤1", "操作步骤2", ...]
           - "expectedResult": "预期结果描述"
           - "webElements": {
               "pageUrl": "目标页面URL或路径",
               "keyElements": ["关键页面元素选择器或描述", ...]
             }

        【需求文档内容】
        {doc_content}
        """
```

### 2.2 测试点转自动化用例转换器 (converters/test_point_to_case.py)

```python
class TestPointToAutomationConverter:
    """将 Web 测试点转换为 Playwright 自动化用例"""
    
    def convert(self, test_point: WebTestPoint) -> AutomationTestCase:
        """
        转换逻辑：
        1. 分析测试点的步骤和页面元素
        2. 结合目标系统实际页面结构
        3. 生成 Playwright 操作序列
        4. 添加断言验证
        """
        
        # LLM Prompt 示例
        prompt = f"""
        你是一个 Web 自动化测试专家。请将以下 Web 测试点转换为 Playwright Python 操作序列。
        
        【测试点信息】
        名称：{test_point.name}
        前置条件：{test_point.preconditions}
        操作步骤：
        {"\n".join([f"- {step}" for step in test_point.steps])}
        预期结果：{test_point.expectedResult}
        页面信息：
        - 页面路径：{test_point.webElements.pageUrl}
        - 关键元素：{"\n- ".join(test_point.webElements.keyElements)}
        
        【目标系统信息】
        基础URL：http://localhost:3000
        
        【转换要求】
        1. 使用 Playwright Python API
        2. 每个步骤包含：操作类型、选择器、参数
        3. 包含必要的等待和断言
        4. 处理常见的页面加载和元素定位问题
        5. 返回 JSON 格式操作序列
        
        【输出格式】
        {{
          "test_case_name": "{test_point.name}_自动化测试",
          "steps": [
            {{"action": "goto", "url": "http://localhost:3000/login"}},
            {{"action": "fill", "selector": "#username", "value": "admin"}},
            {{"action": "click", "selector": "text=登录"}},
            {{"action": "assert", "expected_url_contains": "/projects"}}
          ]
        }}
        """
        
        llm_response = self.llm_client.call(prompt)
        return self._parse_automation_case(llm_response)
```

### 2.3 Playwright 执行引擎 (engines/playwright_executor.py)

```python
class PlaywrightExecutor:
    """执行 Playwright 自动化测试用例"""
    
    def __init__(self, browser_config: BrowserConfig):
        self.browser = self._launch_browser(browser_config)
        self.context = self.browser.new_context()
        self.page = self.context.new_page()
        self.screenshot_manager = ScreenshotManager()
    
    def execute_test_case(self, test_case: AutomationTestCase) -> TestResult:
        """执行完整的测试用例"""
        result = TestResult(test_case.name)
        
        for i, step in enumerate(test_case.steps):
            step_result = self._execute_step(step, step_index=i)
            result.add_step_result(step_result)
            
            # 每步执行后截图
            screenshot = self.screenshot_manager.capture(
                self.page,
                filename=f"step_{i}_{step.action}",
                on_success=step_result.success
            )
            result.add_screenshot(screenshot)
            
            if not step_result.success:
                result.mark_failed(step_result.error)
                break
        
        return result
    
    def _execute_step(self, step: dict) -> StepResult:
        """执行单个步骤"""
        try:
            action = step["action"]
            if action == "goto":
                self.page.goto(step["url"])
                self.page.wait_for_load_state("networkidle")
            elif action == "fill":
                self.page.fill(step["selector"], step["value"])
            elif action == "click":
                self.page.click(step["selector"])
            elif action == "assert":
                self._execute_assertion(step)
            # ... 其他操作
            
            return StepResult(success=True)
        except Exception as e:
            return StepResult(success=False, error=str(e))
```

### 2.4 Allure 报告生成器 (reporters/allure_reporter.py)

```python
class AllureReporter:
    """生成 Allure 测试报告（包含截图）"""
    
    @allure.feature("Web 自动化测试")
    @allure.story("{test_case_name}")
    def report_test_case(self, test_case: AutomationTestCase, result: TestResult):
        """报告测试用例执行结果"""
        
        with allure.step(f"测试用例：{test_case.name}"):
            # 报告每个步骤
            for i, step_result in enumerate(result.step_results):
                with allure.step(f"步骤 {i+1}: {step_result.description}"):
                    # 附加截图
                    if step_result.screenshot:
                        allure.attach(
                            step_result.screenshot.image_data,
                            name=f"步骤{i+1}_截图",
                            attachment_type=allure.attachment_type.PNG
                        )
                    
                    # 失败时抛出异常
                    if not step_result.success:
                        allure.attach(
                            step_result.error_traceback,
                            name="错误堆栈",
                            attachment_type=allure.attachment_type.TEXT
                        )
                        raise AssertionError(step_result.error)
            
            # 附加最终页面状态截图
            if result.final_screenshot:
                allure.attach(
                    result.final_screenshot.image_data,
                    name="最终页面状态",
                    attachment_type=allure.attachment_type.PNG
                )
```

## 3. 目录结构

```
e:\test_ai_jccs/
└── web-automation/
    ├── extractors/                    # 测试点提取
    │   ├── __init__.py
    │   ├── web_test_point_extractor.py   # Web 功能测试点提取
    │   └── doc_parser.py             # 文档解析器
    │
    ├── converters/                    # 转换器
    │   ├── __init__.py
    │   └── test_point_to_case.py     # 测试点 → 自动化用例
    │
    ├── engines/                       # 执行引擎
    │   ├── __init__.py
    │   ├── playwright_executor.py    # Playwright 执行器
    │   └── mcp_client.py             # MCP 客户端
    │
    ├── reporters/                     # 报告生成
    │   ├── __init__.py
    │   └── allure_reporter.py        # Allure 报告
    │
    ├── utils/                         # 工具
    │   ├── __init__.py
    │   ├── screenshot.py             # 截图管理
    │   ├── page_explorer.py          # 页面元素探索
    │   └── config.py                 # 配置
    │
    ├── test_cases/                    # 生成的自动化用例
    │   └── (自动生成的 JSON 文件)
    │
    ├── output/                        # 输出目录
    │   ├── allure-results/           # Allure 原始结果
    │   └── allure-report/            # Allure 最终报告
    │
    ├── requirements.txt
    ├── config.yaml                    # 配置文件
    └── run_full_pipeline.py           # 完整流程入口
```

## 4. 执行流程

```bash
# 一键运行完整流程
python web-automation/run_full_pipeline.py \
  --requirement-doc docs/智能测试平台_V1需求文档.md \
  --target-url http://localhost:3000 \
  --credentials '{"username": "admin", "password": "1"}'

# 内部执行流程：
# 1. 提取测试点
python web-automation/run_full_pipeline.py --stage extract
  └─> WebTestPointExtractor.extract_from_requirement_doc(doc)
  └─> 输出：test_points.json

# 2. 转换为自动化用例
python web-automation/run_full_pipeline.py --stage convert
  └─> TestPointToAutomationConverter.convert(test_points)
  └─> 输出：test_cases/*.json

# 3. 执行自动化测试
python web-automation/run_full_pipeline.py --stage execute
  └─> PlaywrightExecutor.execute_all(test_cases)
  └─> 输出：output/allure-results/

# 4. 生成 Allure 报告
python web-automation/run_full_pipeline.py --stage report
  └─> allure generate output/allure-results -o output/allure-report --clean
  └─> allure open output/allure-report
```

## 5. 核心优势

| 阶段 | 能力 | 说明 |
|------|------|------|
| **提取** | LLM 智能提取 | 从 Web 功能需求自动识别测试点，包含页面元素信息 |
| **转换** | 智能用例生成 | 结合测试点描述 + 页面结构生成可执行用例 |
| **执行** | Playwright MCP | 通过 MCP 协议控制浏览器，支持 CDP 连接 |
| **报告** | Allure + 截图 | 每步执行截图，失败自动附加错误堆栈 |
| **追踪** | 端到端可追溯 | 需求 → 测试点 → 自动化用例 → 执行结果全链路 |

## 6. 技术栈

| 组件 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 执行引擎 | Playwright | 1.40+ | 浏览器自动化 |
| 报告工具 | Allure | 2.20+ | 生成可视化报告 |
| LLM 集成 | Claude API | - | 智能提取和转换 |
| 语言 | Python | 3.8+ | 主要开发语言 |
| 依赖管理 | pip | - | 包管理 |

## 7. 配置文件示例 (config.yaml)

```yaml
# 基础配置
base_url: "http://localhost:3000"
browser:
  type: "chromium"
  headless: true
  slow_mo: 50  # 慢动作，便于调试

# 截图配置
screenshots:
  capture_success: true  # 成功时也截图
  capture_failure: true  # 失败时必截图
  save_dir: "output/screenshots"

# Allure 报告配置
allure:
  results_dir: "output/allure-results"
  report_dir: "output/allure-report"

# LLM 配置
llm:
  api_key: "your_api_key"
  model: "claude-3-opus-20240229"
  temperature: 0.1
  timeout: 60

# 凭据配置
credentials:
  username: "admin"
  password: "1"
```

## 8. 示例测试点提取结果

基于 `智能测试平台_V1需求文档.md` 的提取结果：

```json
[
  {
    "name": "登录 - 使用正确凭据登录成功",
    "description": "前置条件：系统已启动。操作步骤：1) 打开登录页面 2) 输入账号'admin' 3) 输入密码'1' 4) 点击登录按钮。预期结果：进入项目列表页。关注点：1) 登录成功 2) 正确跳转 3) 认证令牌存储",
    "priority": "HIGH",
    "sceneType": "Functional",
    "preconditions": "系统已启动",
    "steps": [
      "打开登录页面",
      "输入账号'admin'",
      "输入密码'1'",
      "点击登录按钮"
    ],
    "expectedResult": "进入项目列表页",
    "webElements": {
      "pageUrl": "/login",
      "keyElements": ["#username", "#password", "text=登录"]
    }
  },
  {
    "name": "项目管理 - 创建新项目",
    "description": "前置条件：已登录系统。操作步骤：1) 进入项目列表页 2) 点击'创建项目'按钮 3) 填写项目名称和描述 4) 点击'确定'。预期结果：项目创建成功，列表显示新项目。关注点：1) 必填项验证 2) 项目名称唯一性 3) 列表实时更新",
    "priority": "HIGH",
    "sceneType": "Functional",
    "preconditions": "已登录系统",
    "steps": [
      "进入项目列表页",
      "点击'创建项目'按钮",
      "填写项目名称和描述",
      "点击'确定'"
    ],
    "expectedResult": "项目创建成功，列表显示新项目",
    "webElements": {
      "pageUrl": "/projects",
      "keyElements": ["text=创建项目", "#projectName", "#projectDescription", "text=确定"]
    }
  }
]
```

## 9. 示例自动化用例

```json
[
  {
    "test_case_name": "登录 - 使用正确凭据登录成功_自动化测试",
    "steps": [
      {
        "action": "goto",
        "url": "http://localhost:3000/login"
      },
      {
        "action": "fill",
        "selector": "#username",
        "value": "admin"
      },
      {
        "action": "fill",
        "selector": "#password",
        "value": "1"
      },
      {
        "action": "click",
        "selector": "text=登录"
      },
      {
        "action": "assert",
        "expected_url_contains": "/projects"
      }
    ]
  }
]
```

## 10. 实施建议

1. **先验证提取能力**：使用现有需求文档测试 LLM 提取效果，调整 Prompt
2. **构建执行引擎**：集成 Playwright MCP，确保浏览器操作稳定
3. **完善报告集成**：确保 Allure 报告包含所有必要信息
4. **增量扩展**：先支持核心功能，再扩展到复杂场景
5. **持续优化**：根据实际执行结果，不断调整提取和转换逻辑

## 11. 后续规划

1. **支持更多页面类型**：复杂表单、数据表格、文件上传等
2. **智能元素定位**：自动处理动态元素和定位策略
3. **并行执行**：支持多浏览器、多设备并行测试
4. **CI/CD 集成**：与 Jenkins、GitLab CI 等集成
5. **智能修复**：自动分析失败原因并尝试修复

---

**文档版本**：v1.0
**创建日期**：2026-04-23
**适用范围**：Web 自动化测试框架设计与实施