# Web自动化测试 MVP 改造实施说明

## 当前系统适配分析

### 已阅读代码

- `docs/Web自动化测试框架设计方案.md`
- `backend/src/main/java/com/testplatform/entity/Requirement.java`
- `backend/src/main/java/com/testplatform/entity/TestPoint.java`
- `backend/src/main/java/com/testplatform/entity/TestCase.java`
- `backend/src/main/java/com/testplatform/entity/ExecutionTask.java`
- `backend/src/main/java/com/testplatform/entity/ExecutionResult.java`
- `backend/src/main/java/com/testplatform/util/TestMetadata.java`
- `backend/src/main/java/com/testplatform/service/RequirementService.java`
- `backend/src/main/java/com/testplatform/service/TestPointService.java`
- `backend/src/main/java/com/testplatform/service/TestCaseService.java`
- `backend/src/main/java/com/testplatform/service/AutomationService.java`
- `backend/src/main/java/com/testplatform/service/AllureReportService.java`
- `backend/src/main/java/com/testplatform/service/LLMService.java`
- `backend/src/main/java/com/testplatform/controller/RequirementController.java`
- `backend/src/main/java/com/testplatform/controller/TestPointController.java`
- `backend/src/main/java/com/testplatform/controller/TestCaseController.java`
- `backend/src/main/java/com/testplatform/controller/AutomationController.java`
- `frontend/src/api.js`
- `frontend/src/components/ProjectWorkbenchConsole.vue`

### 当前系统可直接复用的能力

1. 已有完整主链路：`Requirement -> TestPoint -> TestCase -> ExecutionTask -> ExecutionResult -> Allure`
2. 已有统一类型字段：`testType`、`executionType`、`taskType`、`result`
3. `RequirementService` 已支持从需求文档经 LLM 生成测试点
4. `TestCaseService` 已支持从测试点经 LLM 生成测试用例
5. `AutomationService` 已是统一执行入口，适合改造成按类型分发
6. `AllureReportService` 已有报告生成逻辑，适合增量兼容 Web
7. 前端已有统一请求封装 `frontend/src/api.js`
8. 实际使用的前端工作台 `ProjectWorkbenchConsole.vue` 已预留 `web_automation` 菜单入口

### 需要扩展的模块

1. `TestMetadata`
需要正式兼容 `WEB` 测试类型，并保留 `WEB_AUTOMATION` 执行类型

2. `TestCase`
需要新增结构化字段保存 Web 自动化动作序列

3. `LLMService`
需要补 Web 需求分析 Prompt 和 Web 用例生成 Prompt

4. `RequirementService`
需要在现有需求分析链路中根据 `Requirement.testType=WEB` 路由到 Web 测试点生成

5. `TestCaseService`
需要在现有用例生成链路中根据 `TestPoint.testType=WEB` 生成 Web 自动化用例，并结构化落库

6. `AutomationService`
需要从“仅接口执行”升级为“统一入口 + 按类型分发”

7. `AllureReportService`
需要兼容 Web 类型任务的步骤名称、标签和附件描述

8. `ProjectWorkbenchConsole.vue`
需要把 Web 类型展示、Web 用例执行、报告查看入口真正接上

### 设计文档中不能直接照搬的部分

1. 文档主方案是独立 Python/Playwright 项目，不符合当前仓库的增量改造要求
2. 文档中的目录结构和运行入口是新系统思路，不适合直接塞进当前 Java 平台
3. 当前仓库已经有统一实体、服务、执行任务、Allure 结果模型，应该优先复用而不是重建

### 最小可行落地方案

1. `testType` 正式兼容 `WEB`
2. `executionType` 使用 `WEB_AUTOMATION`
3. 在 `TestCase` 新增 `automationPayload` 保存结构化 Web 动作序列
4. 保留 `steps` 作为前端展示文本
5. `AutomationService` 做统一入口，接口走原逻辑，Web 走新分发骨架
6. `AllureReportService` 做 Web 类型基础兼容
7. 前端仅改实际使用页面 `ProjectWorkbenchConsole.vue`

## MVP 改造方案

### 后端改动文件

- `backend/src/main/java/com/testplatform/util/TestMetadata.java`
- `backend/src/main/java/com/testplatform/entity/TestCase.java`
- `backend/src/main/java/com/testplatform/service/LLMService.java`
- `backend/src/main/java/com/testplatform/service/RequirementService.java`
- `backend/src/main/java/com/testplatform/service/TestCaseService.java`
- `backend/src/main/java/com/testplatform/service/AutomationService.java`
- `backend/src/main/java/com/testplatform/service/AllureReportService.java`

### 前端改动文件

- `frontend/src/components/ProjectWorkbenchConsole.vue`

### 新增或扩展字段

#### `TestCase.automationPayload`

原因：

1. `steps` 继续承担文本展示职责
2. `requestData` 继续服务接口自动化，不与 Web 数据混用
3. `automationPayload` 作为最小新增字段，能承载结构化 Web 步骤而不破坏原模型

建议内容：

```json
{
  "startUrl": "/login",
  "finalUrl": "/projects",
  "finalAssertion": "URL 包含 /projects",
  "steps": [
    {
      "action": "goto",
      "target": "/login",
      "description": "打开登录页"
    },
    {
      "action": "fill",
      "selector": "#username",
      "value": "admin",
      "description": "输入账号"
    }
  ]
}
```

### 新增能力建议

1. `LLMService.analyzeWebRequirement(...)`
2. `LLMService.generateWebTestCases(...)`
3. `AutomationService.executeWebTask(...)`

### 执行链路改造

1. 需求上传时，支持选择 `WEB`
2. `RequirementService.generateTestPoints(...)` 根据 `Requirement.testType` 分流
3. `TestCaseService.generateTestCases(...)` 根据 `TestPoint.testType` 分流
4. `AutomationService.createTask(...)` 和 `executeTask(...)` 统一接收，再按类型执行
5. `AllureReportService.generateReport(...)` 根据任务类型输出 Web 或 API 风格的基础报告

## 第一批代码实现

### 已修改文件清单

- `backend/src/main/java/com/testplatform/util/TestMetadata.java`
- `backend/src/main/java/com/testplatform/entity/TestCase.java`
- `backend/src/main/java/com/testplatform/service/LLMService.java`
- `backend/src/main/java/com/testplatform/service/RequirementService.java`
- `backend/src/main/java/com/testplatform/service/TestCaseService.java`
- `backend/src/main/java/com/testplatform/service/AutomationService.java`
- `backend/src/main/java/com/testplatform/service/AllureReportService.java`
- `frontend/src/components/ProjectWorkbenchConsole.vue`

### 改动说明

#### 1. Web 类型兼容

`TestMetadata.java`

1. 新增 `TEST_TYPE_WEB = "WEB"`
2. 将旧的 `WEB_AUTOMATION` 测试类型归一为 `WEB`
3. 新增 `isWebTest(...)`

#### 2. Web 测试点生成链路

`LLMService.java`

1. 新增 `analyzeWebRequirement(...)`
2. 新增 Web 需求 Prompt 构建逻辑

`RequirementService.java`

1. 根据 `Requirement.testType=WEB` 分流到 Web 需求分析
2. 兼容解析 `preconditions`、`steps`、`expectedResult`、`webElements`
3. 将结构化内容回填到 `description`

#### 3. Web 测试用例结构化存储

`TestCase.java`

1. 新增 `automationPayload`

`LLMService.java`

1. 新增 `generateWebTestCases(...)`
2. 新增 Web 自动化用例 Prompt

`TestCaseService.java`

1. 解析 Web 用例 JSON 中的 `automationPayload`
2. `steps` 支持字符串和数组两种解析
3. Web 默认回退用例会自动生成 `automationPayload`

#### 4. AutomationService 的 Web 执行分发能力骨架

`AutomationService.java`

1. `createTask(...)` 支持 Web 用例创建任务
2. `runnerType` 对 Web 任务标记为 `WEB`
3. `executeTask(...)` 中新增 Web 分发
4. 新增 `executeWebTask(...)` 作为当前 MVP 骨架执行器
5. 当前 Web 执行仍是平台内骨架，不是浏览器真执行

#### 5. Allure 对 Web 执行结果的基础兼容

`AllureReportService.java`

1. 报告标签支持 `Web Automation`
2. 步骤标题支持 Web 风格命名
3. 描述区根据类型切换为 Web 或 API 描述

#### 6. 前端最小接入

`ProjectWorkbenchConsole.vue`

1. 上传需求时支持 `WEB`
2. 测试点和测试用例筛选支持 `WEB`
3. Web 自动化模块从筛选 `FUNCTIONAL` 改为筛选 `WEB`
4. 新增单条和批量 Web 执行方法
5. 保留并接通 Allure 报告查看入口

### 为什么这么改

1. 先优先打通“需求 -> 测试点 -> 用例 -> 任务 -> 报告”的最小闭环
2. 先把 Web 结构化数据纳入当前平台模型，再补真实浏览器执行器
3. 避免大规模重构，所有改动都落在现有实体、服务和页面上

## 当前进度

本轮已完成：

1. `WEB` 类型兼容
2. Web 测试点生成链路
3. Web 测试用例结构化存储
4. `AutomationService` 的 Web 执行分发骨架
5. Allure 对 Web 执行结果的基础兼容
6. 前端最小接入

本轮未完成：

1. 真实浏览器执行器
2. 截图采集
3. 页面 DOM 状态采集
4. 失败截图挂载 Allure

## 验证方法

### 已完成验证

1. 后端编译验证通过

```bash
mvn -q -DskipTests compile
```

2. 前端构建验证通过

```bash
npm.cmd run build
```

### 手动验证建议

1. 前端上传一个 `WEB` 类型需求文档
2. 调用测试点生成，确认生成结果中 `testType=WEB`
3. 基于 Web 测试点生成测试用例
4. 打开测试用例详情，确认：
   - `testType=WEB`
   - `executionType=WEB_AUTOMATION`
   - `steps` 有展示文本
   - `automationPayload` 已落库
5. 在 Web 自动化模块发起执行
6. 执行后检查：
   - `ExecutionTask.taskType=WEB`
   - `ExecutionTask.runnerType=WEB`
   - `ExecutionResult.testType=WEB`
   - `reportUrl` 可打开

## 下一步计划

### 第二批建议实现

1. 将 `executeWebTask(...)` 从骨架升级为真实浏览器执行器
2. 在 `automationPayload` 中补充截图步骤与页面断言
3. 将截图作为附件写入 Allure
4. 补充失败时页面状态、错误步骤、错误截图
5. 前端补充 `automationPayload` 可视化查看

### 建议下一条指令

```text
继续第二批实现，接入真实 Web 执行器，并补截图与 Allure 附件
```
