# 需求测试类型扩展与 Web 自动化测试模块详细设计方案

## 1. 文档目的

本文档用于评审以下两类需求的整体改造方案，不直接修改代码：

1. 在上传需求时新增测试类型选择，用于区分`功能测试`和`接口测试`
2. 在现有系统上新增`Web 自动化测试`模块，形成与现有接口测试并列的执行能力

本文档基于当前代码现状进行设计，目标是：

1. 保证存量数据兼容
2. 保证现有接口测试链路不被破坏
3. 为后续功能测试和 Web 自动化测试扩展预留统一模型
4. 把页面、接口、数据、流程、迁移、风险全部说清楚，便于你评审后决定是否进入开发

---

## 2. 当前系统现状分析

### 2.1 当前核心对象

当前后端核心对象如下：

1. `Requirement`
   现有字段主要包含：`projectId/name/fileUrl/version/status/testPointCount`
2. `TestPoint`
   现有字段主要包含：`requirementId/pointType/name/sceneType/riskLevel/description`
3. `TestCase`
   现有字段主要包含：`testPointId/caseType/title/precondition/steps/expectedResult/requestData/automation/status`
4. `ExecutionTask`
   现有字段主要包含：`projectId/testCaseId/taskType/runnerType/environment/status`
5. `ExecutionResult`
   现有字段主要包含：`requestUrl/requestMethod/requestBody/responseBody/assertionResult/llmAnalysis/reportUrl`

### 2.2 当前业务主链路

当前系统实际上只有一条主链路：

1. 上传需求文档
2. 调用 LLM 生成测试点
3. 基于测试点调用 LLM 生成测试用例
4. 在“接口测试”模块中创建执行任务
5. 根据 `requestData` 或步骤解析 HTTP 请求
6. 本地直接发起接口调用并生成 Allure 报告

### 2.3 当前实现特点

1. `Requirement` 上没有“测试类型”字段，系统无法区分功能测试需求和接口测试需求
2. `TestPoint.pointType` 当前基本被写死为 `FUNCTIONAL`，但生成逻辑又明显偏接口文档解析，语义不一致
3. `TestCase.caseType` 默认值是 `FUNCTIONAL`，但很多用例又带有 `requestData`，实际上是接口测试用例
4. `ExecutionTask.taskType` 默认是 `API_TEST`
5. “接口测试”页面当前读取的是全部测试用例，并未按“接口测试用例”过滤
6. 当前自动化执行引擎仅支持 HTTP 请求回放，不支持浏览器驱动、页面元素定位、页面断言、截图和步骤回放
7. 数据库当前采用 `spring.jpa.hibernate.ddl-auto=update`，没有显式版本化迁移工具

### 2.4 当前系统的核心问题

如果在现状上只加一个前端下拉框，不补齐后端模型和执行边界，会出现以下问题：

1. 功能测试和接口测试会继续共用一套模糊字段，后续逻辑会越来越混乱
2. 存量数据无法明确归类，页面筛选和执行入口会互相污染
3. Web 自动化模块没有独立任务、结果、脚本和报告模型，最终会把接口测试模块做坏
4. 将来如果再扩展 App 自动化、性能测试，会继续重复返工

因此，本次设计建议不是“只加一个下拉框”，而是顺势把“测试类型”上升为系统一级概念。

---

## 3. 设计目标

### 3.1 业务目标

1. 上传需求时，必须可选择测试类型：`功能测试`、`接口测试`
2. 所有历史已存在需求，默认归类为`接口测试`
3. 历史需求后续从测试点提取、用例生成、接口测试执行，全部按原有接口测试业务逻辑继续执行
4. 新增独立的 `Web 自动化测试` 模块，不影响现有接口测试模块
5. 为后续“功能测试 -> Web 自动化用例 -> Web 执行”建立可演进的架构

### 3.2 技术目标

1. 数据模型统一，有明确测试类型主线
2. 存量兼容，不要求一次性重洗所有旧数据
3. 页面入口清晰，用户一眼能分辨不同类型
4. 接口测试与 Web 自动化测试的任务、执行器、报告结构解耦
5. 后续可逐步演进，不要求首版就做到全量智能生成脚本

---

## 4. 总体设计原则

### 4.1 类型上移原则

测试类型不能只存在前端表单中，必须在：

1. 需求
2. 测试点
3. 测试用例
4. 执行任务
5. 执行结果

这五层对象中形成贯通字段。

### 4.2 存量兼容原则

所有历史数据默认归类为`接口测试`，并保持当前可继续使用。也就是说：

1. 历史 `Requirement` 默认 `testType=INTERFACE`
2. 历史 `TestPoint` 默认 `testType=INTERFACE`
3. 历史 `TestCase` 默认 `testType=INTERFACE`
4. 历史 `ExecutionTask.taskType` 保持或映射为 `INTERFACE`

### 4.3 执行器隔离原则

接口测试与 Web 自动化测试必须是两套执行器：

1. 接口测试执行器：HTTP 回放
2. Web 自动化执行器：浏览器驱动执行

二者共享任务框架，但不能混用执行细节字段。

### 4.4 首版可落地原则

Web 自动化模块首版建议分两期：

1. V1：先完成模块、脚本管理、任务调度、报告查看、基础执行能力
2. V2：再增加 LLM 自动生成脚本、录制回放、页面对象建模、数据驱动等增强能力

这样上线风险更低。

---

## 5. 目标业务模型

### 5.1 统一测试类型枚举

建议全系统统一新增枚举：

| 枚举值 | 中文 | 说明 |
|---|---|---|
| `FUNCTIONAL` | 功能测试 | 指面向业务功能、流程、页面、交互的测试 |
| `INTERFACE` | 接口测试 | 指基于接口文档、请求参数、响应断言的测试 |
| `WEB_AUTOMATION` | Web自动化 | 指基于浏览器执行的自动化用例类型 |

说明：

1. `FUNCTIONAL` 用于需求、测试点、手工/功能用例归类
2. `INTERFACE` 用于现有链路归类
3. `WEB_AUTOMATION` 用于执行模块中的自动化用例归类

注意：`WEB_AUTOMATION` 不建议直接出现在“上传需求下拉框”里。上传需求场景只区分业务意图：

1. 功能测试
2. 接口测试

而 `WEB_AUTOMATION` 是从功能测试用例进一步派生的自动化执行能力。

---

## 6. 需求上传模块改造设计

### 6.1 前端页面改造

当前“上传需求文档”弹窗已有字段：

1. 项目
2. 需求名称
3. 版本号
4. 上传文件

本次新增字段：

1. `测试类型` 下拉框，必填

下拉选项：

1. `功能测试`
2. `接口测试`

默认值建议：

1. 新上传需求`不设置默认值`，要求用户显式选择

原因：

1. 你的目标是明确区分功能测试和接口测试，显式选择最不容易出错
2. 历史数据已经统一兜底为接口测试，不会影响旧数据
3. 如果你们业务上绝大多数都是接口文档，也可以改成默认 `接口测试`，但这应作为产品决策明确确认

### 6.2 交互规则

1. 未选择测试类型时，不允许提交
2. 需求列表页新增“测试类型”列
3. 支持按测试类型筛选需求
4. 不同测试类型的需求，其“提取测试点”按钮文案不变，但后续调用不同后端逻辑

### 6.3 后端接口改造

当前上传接口：

`POST /api/requirements/upload`

当前参数：

1. `projectId`
2. `file`
3. `name`
4. `version`

新增参数：

1. `testType`

请求示例：

```http
POST /api/requirements/upload
Content-Type: multipart/form-data
```

表单字段：

1. `projectId=1`
2. `name=用户中心需求`
3. `version=v1.0`
4. `testType=FUNCTIONAL`
5. `file=xxx.docx`

### 6.4 后端对象改造

`Requirement` 新增字段：

1. `testType`：需求测试类型

建议字段定义：

1. 类型：`String`
2. 非空
3. 默认值：`INTERFACE`

### 6.5 生成测试点逻辑改造

当前 `RequirementService.generateTestPoints` 是：

1. 先读需求内容
2. 自动判断是不是接口文档
3. 如果像接口文档，走接口文档分析 prompt
4. 否则走通用需求分析 prompt

本次建议改造为“显式类型优先，自动识别兜底”：

1. 若 `requirement.testType=INTERFACE`
   直接走接口文档测试点提取逻辑
2. 若 `requirement.testType=FUNCTIONAL`
   直接走功能测试点提取逻辑
3. 若历史脏数据为空
   再走原有自动识别逻辑兜底

这样可以避免用户明明选了“功能测试”，系统又因为文档里出现接口关键字被错误识别成接口测试。

---

## 7. 测试点模块改造设计

### 7.1 数据模型改造

`TestPoint` 建议新增字段：

1. `testType`
2. `sourceType`

字段说明：

1. `testType`
   继承自需求测试类型，值为 `FUNCTIONAL` 或 `INTERFACE`
2. `sourceType`
   表示测试点来源，建议取值：
   `LLM_GENERATED` / `MANUAL_CREATED` / `MIGRATED`

### 7.2 生成规则

#### 7.2.1 接口测试需求

生成出的测试点：

1. `testType=INTERFACE`
2. `pointType=INTERFACE`
3. 名称优先对齐接口名
4. 描述中保留接口路径、方法、认证、核心参数、响应断言关注点

#### 7.2.2 功能测试需求

生成出的测试点：

1. `testType=FUNCTIONAL`
2. `pointType=FUNCTIONAL`
3. 场景可覆盖：正常流、异常流、边界值、权限、兼容、易用性
4. 描述偏业务步骤和规则，不生成 HTTP 请求细节

### 7.3 页面改造

测试点列表建议新增：

1. 测试类型列
2. 来源类型列
3. 筛选项增加“测试类型”

### 7.4 页面行为规则

1. 来源于接口测试需求的测试点，后续默认进入接口用例生成链路
2. 来源于功能测试需求的测试点，后续默认进入功能用例生成链路
3. 手工新增测试点时，必须显式选择测试类型，默认继承当前筛选上下文或关联需求的测试类型

---

## 8. 测试用例模块改造设计

### 8.1 数据模型改造

`TestCase` 建议新增或规范化以下字段：

1. `testType`
2. `executionType`
3. `scriptRefId`
4. `requestData`

字段说明：

1. `testType`
   用例所属测试类型，值建议：
   `FUNCTIONAL` / `INTERFACE` / `WEB_AUTOMATION`
2. `executionType`
   用例执行方式，值建议：
   `MANUAL` / `API_AUTOMATION` / `WEB_AUTOMATION`
3. `scriptRefId`
   若是 Web 自动化用例，关联自动化脚本主键
4. `requestData`
   仅接口测试用例使用

### 8.2 用例生成规则

#### 8.2.1 接口测试点 -> 接口测试用例

沿用当前业务逻辑，只做字段补齐：

1. `testType=INTERFACE`
2. `caseType=INTERFACE`
3. `executionType=API_AUTOMATION`
4. `requestData` 按现有逻辑生成

这部分必须保证与当前行为一致。

#### 8.2.2 功能测试点 -> 功能测试用例

生成规则：

1. `testType=FUNCTIONAL`
2. `caseType=FUNCTIONAL`
3. `executionType=MANUAL`
4. 不强制生成 `requestData`
5. `steps` 聚焦页面操作、业务步骤、校验点
6. `expectedResult` 聚焦页面表现、业务结果、提示信息、数据变化

### 8.3 用例列表页面改造

建议增加：

1. 测试类型列
2. 执行方式列
3. 支持按测试类型筛选
4. 操作列根据类型显示不同按钮

按钮建议：

1. 接口测试用例：`加入接口执行`
2. 功能测试用例：`转Web自动化`
3. Web自动化用例：`立即执行`

### 8.4 兼容要求

所有旧用例默认按下述规则兼容：

1. 若历史用例存在 `requestData`
   默认 `testType=INTERFACE`
2. 若无法判断
   统一默认 `testType=INTERFACE`

原因：

1. 用户要求所有已完成系统默认全部选中接口测试
2. 当前系统的执行能力本身也只覆盖接口测试

---

## 9. 接口测试链路兼容设计

这是本次最关键的一条红线。

### 9.1 兼容目标

对历史需求、历史测试点、历史用例、历史接口执行任务，必须做到：

1. 页面可见
2. 可继续生成测试点
3. 可继续生成用例
4. 可继续接口执行
5. 可继续查看历史 Allure 报告

### 9.2 兼容策略

#### 9.2.1 需求层

历史 `Requirement.testType` 为空时：

1. 数据迁移脚本回填为 `INTERFACE`
2. 代码中仍增加空值兜底

#### 9.2.2 测试点层

历史 `TestPoint.testType` 为空时：

1. 优先继承所属 `Requirement.testType`
2. 若 requirement 不存在，默认 `INTERFACE`

#### 9.2.3 用例层

历史 `TestCase.testType` 为空时：

1. 若 `requestData` 非空，回填 `INTERFACE`
2. 否则仍默认 `INTERFACE`

#### 9.2.4 执行层

历史 `ExecutionTask.taskType` 若为空：

1. 默认 `INTERFACE`

### 9.3 接口测试页面改造原则

当前“接口测试”页面读取全部测试用例，这会带来污染。改造后必须：

1. 仅展示 `testType=INTERFACE` 或 `executionType=API_AUTOMATION` 的用例
2. 批量执行时仅允许勾选接口测试用例
3. 执行服务仅消费接口测试任务

### 9.4 接口执行服务改造原则

当前 `AutomationService` 本质上是接口执行服务，建议重构为：

1. `ExecutionTaskService`
2. `ApiAutomationExecutor`
3. `WebAutomationExecutor`

其中：

1. `ExecutionTaskService` 负责创建任务、状态流转、统一调度
2. `ApiAutomationExecutor` 负责现有 HTTP 执行逻辑
3. `WebAutomationExecutor` 负责新浏览器执行逻辑

这样既能保持现有逻辑，又能为新模块接入。

---

## 10. Web 自动化测试模块设计

## 10.1 模块定位

Web 自动化测试模块不是接口测试页面上的一个按钮，而是独立一级模块，建议在左侧菜单新增：

1. `Web自动化`

模块定位：

1. 面向页面和业务流的自动化执行
2. 支持脚本管理、任务执行、执行报告、失败分析
3. 与接口测试共享项目、需求、测试点、用例，但拥有独立执行器

### 10.2 模块边界

#### 本次纳入范围

1. Web 自动化用例列表
2. Web 自动化脚本管理
3. Web 自动化任务创建
4. Web 自动化任务执行
5. 执行日志、截图、报告查看
6. 从功能测试用例一键转化为 Web 自动化用例

#### 本次暂不纳入范围

1. 浏览器录制回放
2. 页面对象模型可视化维护
3. 云端分布式执行
4. 多浏览器并行矩阵
5. AI 自动修复定位器

这些作为后续增强项更合理。

---

## 11. Web 自动化领域模型设计

### 11.1 新增表：`web_automation_case`

建议职责：

1. 作为 Web 自动化用例主表

建议字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| project_id | 所属项目 |
| requirement_id | 来源需求 |
| test_point_id | 来源测试点 |
| test_case_id | 来源功能测试用例 |
| case_name | 自动化用例名称 |
| case_code | 自动化用例编号 |
| status | 草稿/已发布/停用 |
| priority | 优先级 |
| script_id | 绑定脚本 |
| page_url | 入口地址 |
| tags | 标签 |
| creator | 创建人 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 11.2 新增表：`web_automation_script`

建议职责：

1. 存储脚本元数据，不直接把大段代码塞进普通用例表

建议字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| project_id | 所属项目 |
| script_name | 脚本名称 |
| script_type | `PLAYWRIGHT` |
| language | `JS`/`TS` |
| script_content | 脚本内容 |
| locator_strategy | 定位策略摘要 |
| version | 脚本版本 |
| status | 草稿/可执行/废弃 |
| created_by | 创建人 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

说明：

1. 首版建议统一 Playwright
2. 不建议首版同时支持 Selenium 和 Cypress，会显著增加复杂度

### 11.3 新增表：`web_automation_task`

建议职责：

1. 存储 Web 自动化执行任务

建议字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| task_no | 任务编号 |
| project_id | 所属项目 |
| web_case_id | Web自动化用例ID |
| script_id | 执行脚本ID |
| environment | 执行环境 |
| browser | chrome/edge/firefox |
| base_url | 被测地址 |
| status | PENDING/RUNNING/PASSED/FAILED |
| trigger_mode | 手工/批量/计划任务 |
| started_at | 开始时间 |
| finished_at | 结束时间 |
| created_by | 创建人 |
| created_at | 创建时间 |

### 11.4 新增表：`web_automation_result`

建议职责：

1. 存储 Web 自动化执行结果

建议字段：

| 字段 | 说明 |
|---|---|
| id | 主键 |
| task_id | 任务ID |
| result | PASSED/FAILED/BROKEN |
| summary | 结果摘要 |
| step_log | 步骤日志 |
| screenshot_urls | 截图集合 |
| video_url | 执行录像 |
| trace_url | Playwright trace |
| error_message | 错误信息 |
| report_url | 报告地址 |
| llm_analysis | AI失败分析 |
| executed_at | 执行时间 |

### 11.5 是否复用现有 `ExecutionTask/ExecutionResult`

建议结论：

1. 不直接复用表结构
2. 可以复用统一服务接口和页面样式，但数据表建议拆分

原因：

1. 现有 `ExecutionResult` 明显偏 HTTP 模型
2. Web 自动化需要截图、trace、video、步骤日志
3. 硬塞到一张表里会让字段大量空置、语义混乱

如果你们强烈希望统一表，也可以做“父任务表 + 子结果表”模式，但首版复杂度会更高。

---

## 12. Web 自动化业务流程设计

### 12.1 用例来源流程

建议主流程：

1. 上传功能测试需求
2. 生成功能测试点
3. 生成功能测试用例
4. 在功能测试用例上点击 `转Web自动化`
5. 系统生成 Web 自动化用例和默认脚本草稿
6. 测试人员补充或调整脚本
7. 创建执行任务
8. 执行浏览器自动化
9. 生成结果、截图、报告

### 12.2 脚本生成策略

首版建议支持两种来源：

1. 手工新建脚本
2. 基于功能测试用例由 LLM 生成脚本草稿

LLM 生成脚本时输入上下文：

1. 用例标题
2. 前置条件
3. 测试步骤
4. 预期结果
5. 页面入口地址
6. 可选页面元素描述

输出目标：

1. 标准 Playwright 脚本
2. 包含步骤注释
3. 包含断言
4. 包含失败时截图逻辑

### 12.3 任务执行流程

1. 用户选择环境、浏览器、Base URL
2. 系统创建 `web_automation_task`
3. 调用执行器启动 Playwright 运行
4. 实时采集日志、截图、trace
5. 持久化 `web_automation_result`
6. 生成 HTML/Allure 报告
7. 页面展示执行结果并支持重试

---

## 13. 前端详细改造设计

### 13.1 左侧导航改造

当前导航：

1. 需求管理
2. 测试点
3. 用例管理
4. 接口测试

建议调整为：

1. 需求管理
2. 测试点
3. 用例管理
4. 接口测试
5. Web自动化

### 13.2 需求管理页面

新增：

1. 上传弹窗中的“测试类型”下拉框
2. 列表“测试类型”列
3. 筛选项“测试类型”

建议展示文案：

1. `功能测试`
2. `接口测试`

### 13.3 测试点页面

新增：

1. 测试类型列
2. 按测试类型筛选
3. 不同类型显示不同图标/标签色

### 13.4 用例管理页面

用例管理建议拆成三个视图：

1. 全部用例
2. 功能测试用例
3. 接口测试用例

如果不想加页签，也至少要增加：

1. 测试类型筛选
2. 执行方式筛选

### 13.5 接口测试页面

改造要求：

1. 只读取接口测试用例
2. 页面标题和文案保留“接口测试”
3. 执行记录显示任务类型为 `接口测试`

### 13.6 Web 自动化页面

建议包含四个子页签：

1. `自动化用例`
2. `脚本管理`
3. `执行任务`
4. `执行报告`

#### 自动化用例

展示字段：

1. 用例编号
2. 用例名称
3. 来源功能用例
4. 脚本状态
5. 最近执行结果
6. 操作：编辑脚本、立即执行、查看报告

#### 脚本管理

展示字段：

1. 脚本名称
2. 类型
3. 版本
4. 状态
5. 更新时间

功能：

1. 新建脚本
2. 编辑脚本
3. LLM 生成草稿
4. 保存版本

#### 执行任务

展示字段：

1. 任务编号
2. 用例名称
3. 浏览器
4. 环境
5. 执行状态
6. 开始/结束时间

#### 执行报告

展示内容：

1. 执行摘要
2. 步骤日志
3. 失败截图
4. 视频/trace 链接
5. AI 分析结果

---

## 14. 后端接口设计

## 14.1 需求管理接口变更

### 上传需求

`POST /api/requirements/upload`

新增参数：

1. `testType`

### 查询需求列表

建议支持筛选参数：

1. `projectId`
2. `status`
3. `testType`

### 测试点提取

`POST /api/requirements/{id}/generate-test-points`

行为改造：

1. 读取 `Requirement.testType`
2. 按类型调用不同 prompt

---

## 14.2 用例生成接口变更

当前接口：

1. `POST /api/test-cases/generate`
2. `POST /api/test-cases/generate/batch`

建议改造：

1. 由测试点的 `testType` 自动决定生成哪类用例
2. 返回结果增加 `testType` 和 `executionType`

---

## 14.3 Web 自动化模块新增接口

### 自动化用例

1. `GET /api/web-automation/cases`
2. `POST /api/web-automation/cases`
3. `PUT /api/web-automation/cases/{id}`
4. `DELETE /api/web-automation/cases/{id}`
5. `POST /api/web-automation/cases/{id}/generate-script`

### 脚本管理

1. `GET /api/web-automation/scripts`
2. `POST /api/web-automation/scripts`
3. `PUT /api/web-automation/scripts/{id}`
4. `GET /api/web-automation/scripts/{id}`
5. `POST /api/web-automation/scripts/{id}/publish`

### 执行任务

1. `POST /api/web-automation/tasks`
2. `POST /api/web-automation/tasks/batch`
3. `POST /api/web-automation/tasks/{id}/execute`
4. `POST /api/web-automation/tasks/{id}/retry`
5. `GET /api/web-automation/tasks`
6. `GET /api/web-automation/results/{taskId}`

### 报告

1. `GET /api/web-automation/reports/{taskId}`

---

## 15. LLM 逻辑改造设计

### 15.1 测试点提取 Prompt 分流

建议拆成两套明确 prompt：

1. `analyzeFunctionalRequirement`
2. `analyzeApiRequirement`

不要继续靠一套 prompt 模糊兼容。

### 15.2 用例生成 Prompt 分流

建议拆成三套：

1. `generateFunctionalTestCases`
2. `generateApiTestCases`
3. `generateWebAutomationScriptDraft`

### 15.3 失败分析 Prompt 分流

建议拆成：

1. 接口执行分析 prompt
2. Web 自动化执行分析 prompt

Web 自动化失败分析需要输入：

1. 页面步骤日志
2. 失败截图
3. 浏览器报错
4. 定位器失败信息
5. 预期结果

---

## 16. 数据库变更设计

由于当前项目使用 JPA 自动更新，但正式落地建议补充显式 SQL 脚本，至少做到可审计。

### 16.1 既有表新增字段

#### requirement

新增：

1. `test_type varchar(32) not null default 'INTERFACE'`

#### test_point

新增：

1. `test_type varchar(32) not null default 'INTERFACE'`
2. `source_type varchar(32) not null default 'MIGRATED'`

#### test_case

新增：

1. `test_type varchar(32) not null default 'INTERFACE'`
2. `execution_type varchar(32) not null default 'API_AUTOMATION'`
3. `script_ref_id bigint null`

#### execution_task

建议统一规范：

1. `task_type varchar(32) not null default 'INTERFACE'`

### 16.2 新增表

1. `web_automation_case`
2. `web_automation_script`
3. `web_automation_task`
4. `web_automation_result`

### 16.3 存量迁移脚本

建议迁移 SQL 包含：

1. requirement 全量回填 `test_type='INTERFACE'`
2. test_point 全量回填 `test_type='INTERFACE'`
3. test_case 全量回填 `test_type='INTERFACE'`
4. test_case 全量回填 `execution_type='API_AUTOMATION'`
5. execution_task 全量回填 `task_type='INTERFACE'`

### 16.4 迁移策略

建议分两步：

1. 先发版字段和兜底逻辑
2. 再执行数据回填脚本

这样即使脚本延后执行，系统也不会因为空值直接报错。

---

## 17. 详细代码改造建议

### 17.1 后端改造点

#### 实体层

需要改造：

1. `Requirement`
2. `TestPoint`
3. `TestCase`
4. `ExecutionTask`

需要新增：

1. `WebAutomationCase`
2. `WebAutomationScript`
3. `WebAutomationTask`
4. `WebAutomationResult`

#### Repository 层

新增按类型查询能力：

1. Requirement 按 `testType` 查询
2. TestPoint 按 `testType` 查询
3. TestCase 按 `testType/executionType` 查询

#### Service 层

建议拆分：

1. `RequirementService`
   增加 `testType` 保存、分流生成测试点
2. `TestCaseService`
   增加按类型生成用例
3. `ExecutionTaskService`
   统一任务编排
4. `ApiAutomationService`
   保留现有接口执行逻辑
5. `WebAutomationService`
   新增浏览器执行逻辑

#### Controller 层

1. 现有 Requirement/TestCase/Automation Controller 补充类型参数与过滤能力
2. 新增 `WebAutomationController`

### 17.2 前端改造点

#### `ProjectWorkbenchConsole.vue`

这是当前工作台主页面，建议重点改造：

1. 需求上传表单增加 `testType`
2. 需求列表增加 `testType`
3. 筛选条件增加 `testType`
4. 测试点列表增加 `testType`
5. 用例列表增加 `testType`、`executionType`
6. 接口测试模块只展示接口用例
7. 新增 `web_automation` 模块入口及页面视图

#### API 层

`frontend/src/api.js` 无需大改，但要补充新的调用封装和错误提示。

---

## 18. 推荐实施方案

### 18.1 分阶段实施

#### 第一阶段：类型体系落地

范围：

1. 上传需求增加测试类型
2. Requirement/TestPoint/TestCase/ExecutionTask 增加类型字段
3. 历史数据默认回填接口测试
4. 现有接口测试链路保持可用
5. 页面增加类型展示和筛选

产出：

1. 系统能稳定区分功能测试和接口测试
2. 不引入 Web 自动化执行风险

#### 第二阶段：Web 自动化模块首版

范围：

1. Web 自动化菜单和页面
2. 自动化用例、脚本、任务、结果四类对象
3. Playwright 执行器
4. 结果报告和截图查看

#### 第三阶段：智能增强

范围：

1. LLM 自动生成 Playwright 脚本
2. 功能测试用例一键转自动化脚本
3. AI 失败分析

### 18.2 为什么推荐分阶段

1. 你的第一诉求是“分类”和“存量兼容”，这部分必须先稳
2. Web 自动化本身是新能力，涉及执行器、环境依赖、脚本治理，风险明显高于页面改造
3. 分阶段上线更适合当前这套系统的成熟度

### 18.3 评审待确认项

以下项目建议在进入开发前明确拍板：

1. 新上传需求的测试类型下拉框是否允许默认值
   我的建议：不设默认值，强制用户选择
2. 功能测试需求是否允许后续继续生成接口测试用例
   我的建议：首版不允许混生，保持链路单一
3. Web 自动化首版是否要支持脚本 AI 自动生成
   我的建议：支持“生成草稿”，但不作为强依赖能力
4. Web 自动化报告是否与当前 Allure 报告统一
   我的建议：统一报告入口即可，底层结果模型分离

---

## 19. 风险与应对

### 19.1 风险：旧数据混杂导致展示异常

应对：

1. 所有旧数据默认回填 `INTERFACE`
2. 代码层保留空值兜底

### 19.2 风险：现有接口测试被新逻辑误伤

应对：

1. 接口测试链路保持原服务逻辑不变
2. 先做字段透传和过滤，不先重写执行器

### 19.3 风险：Web 自动化模块一次做太大

应对：

1. 首版只支持 Playwright
2. 首版只支持单浏览器本地执行
3. 首版不做录制、不做分布式

### 19.4 风险：LLM 生成脚本不稳定

应对：

1. 首版先允许人工维护脚本
2. LLM 仅生成草稿，不直接强依赖

---

## 20. 验收标准

### 20.1 需求管理

1. 上传需求弹窗可选择测试类型
2. 新需求保存后能在列表中看到测试类型
3. 历史需求全部显示为接口测试

### 20.2 测试点与用例

1. 接口测试需求生成的测试点、用例均为接口类型
2. 功能测试需求生成的测试点、用例均为功能类型
3. 历史接口测试链路不受影响

### 20.3 接口测试

1. 接口测试页面仅展示接口类型用例
2. 历史用例可继续执行
3. 历史报告可继续查看

### 20.4 Web 自动化

1. 可创建 Web 自动化用例
2. 可维护脚本
3. 可执行并生成结果
4. 可查看日志、截图、报告

---

## 21. 最终建议结论

从高级工程师视角，我建议采用以下方案：

1. 把“测试类型”提升为系统主数据，不要只做前端下拉框
2. 存量数据全部默认归为`接口测试`，保证现有链路零中断
3. 接口测试模块保持现有逻辑，仅增加类型过滤和字段透传
4. Web 自动化模块必须独立设计，不建议硬塞进现有接口执行模型
5. 落地顺序采用“两步走”：
   第一步先做类型体系和兼容改造
   第二步再上 Web 自动化模块

如果你认可这个方向，下一步我可以继续为你输出两份内容中的任意一份：

1. 更细一层的“数据库变更 SQL + 接口清单 + 字段字典”
2. 基于当前仓库结构的“逐文件修改清单”，明确每个类、每个页面具体怎么改
