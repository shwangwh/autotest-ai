import com.testplatform.service.RequirementService;
import com.testplatform.entity.TestPoint;

import java.util.List;

public class TestRequirementService {
    public static void main(String[] args) {
        // 创建 RequirementService 实例
        RequirementService requirementService = new RequirementService(null, null, null);
        
        // 模拟 LLM 返回的 Markdown 格式结果
        String llmResult = "{
  \"output\": {
    \"choices\": [
      {
        \"finish_reason\": \"stop\",
        \"message\": {
          \"content\": [
            {
              \"text\": \"根据提供的《智能测试平台 - 系统接口文档》，我为您提取了详细的接口测试点。测试点分为**全局通用测试**、**认证模块测试**、**项目管理模块测试**以及**安全与性能测试**四个部分。\n\n### 一、全局通用测试点 (Global)\n基于文档的\"全局说明\"部分，所有接口均需遵守的规范。\n\n| 编号 | 测试项 | 测试内容/预期结果 |\n| :--- | :--- | :--- |\n| **G-01** | **基础路径验证** | 验证所有接口请求路径是否均以 `/api` 开头。 |\n| **G-02** | **响应结构验证** | 验证所有接口返回的 JSON 是否包含 `success` (Boolean), `message` (String), `data` (Object/Null) 字段。 |\n| **G-03** | **Content-Type 验证** | 验证请求头 `Content-Type` 是否为 `application/json`，否则应返回格式错误。 |\n| **G-04** | **HTTP 方法验证** | 对仅支持 POST 的接口尝试发送 GET/PUT/DELETE 请求，预期返回 405 Method Not Allowed 或相应错误。 |\n| **G-05** | **异常响应格式** | 触发服务器内部错误（如 500），验证返回格式是否仍保持统一的 JSON 结构（而非 HTML 报错页）。 |\n| **G-06** | **分页参数默认值** | (针对列表接口) 不传 `page` 和 `size` 参数，验证是否默认使用 `page=1`, `size=10`。 |\n| **G-07** | **分页参数类型校验** | (针对列表接口) 传入非整数、负数、0 作为分页参数，验证系统是否有容错处理或返回错误提示。 |\n\n---\n\n### 二、认证模块测试点 (Auth - 用户登录)\n**接口**: `POST /api/auth/login`\n\n| 编号 | 测试类型 | 测试场景 | 预期结果 |\n| :--- | :--- | :--- | :--- |\n| **A-01** | **正常场景** | 输入正确的 `username` 和 `password` | `success=true`, 返回 `authorization` 令牌，`message` 提示成功。 |\n| **A-02** | **必填项校验** | 不传 `username` 参数 | `success=false`, 提示缺少用户名参数。 |\n| **A-03** | **必填项校验** | 不传 `password` 参数 | `success=false`, 提示缺少密码参数。 |\n| **A-04** | **空值校验** | `username` 或 `password` 为空字符串 `\"\"` | `success=false`, 提示参数不能为空。 |\n| **A-05** | **逻辑校验** | 用户名存在，密码错误 | `success=false`, `message` 提示 \"Invalid username or password.\"。 |\n| **A-06** | **逻辑校验** | 用户名不存在 | `success=false`, `message` 提示 \"Invalid username or password.\" (建议不区分具体错误以防枚举用户)。 |\n| **A-07** | **响应数据校验** | 登录成功后，检查 `authorization` 格式 | 格式应为 `Basic Base64 编码字符串`。 |\n| **A-08** | **响应数据校验** | 登录成功后，检查返回的 `username` | 应与请求输入的 `username` 一致。 |\n| **A-09** | **特殊字符** | 用户名或密码包含特殊字符 (如 `'`, `<`, `>`) | 系统正常处理，无 SQL 注入或 XSS 报错。 |\n| **A-10** | **长度边界** | 用户名/密码超过数据库定义的最大长度 | 返回参数长度错误提示。 |\n\n---\n\n### 三、项目管理模块测试点 (Project - 创建项目)\n**接口**: `POST /api/projects`\n\n| 编号 | 测试类型 | 测试场景 | 预期结果 |\n| :--- | :--- | :--- | :--- |\n| **P-01** | **鉴权验证** | **不携带** `Authorization` 请求头 | 返回 401 Unauthorized 或 `success=false` 提示未认证。 |\n| **P-02** | **鉴权验证** | 携带**无效/过期**的 `Authorization` 令牌 | 返回 401/403 或提示认证失败。 |\n| **P-03** | **鉴权验证** | 携带登录接口获取的**有效**令牌 | 请求通过，进入业务逻辑校验。 |\n| **P-04** | **正常场景** | 输入合法的 `name`，可选填 `description`, `owner` | `success=true`, `data` 返回创建后的项目对象。 |\n| **P-05** | **必填项校验** | 不传 `name` 参数 | `success=false`, 提示项目名称为必填项。 |\n| **P-06** | **空值校验** | `name` 为空字符串 `\"\"` 或 仅包含空格 | `success=false`, 提示名称不能为空。 |\n| **P-07** | **可选参数** | 不传 `description` 和 `owner` | 创建成功，数据库中对应字段为 null 或默认值。 |\n| **P-08** | **可选参数** | `description` 或 `owner` 传 `null` | 创建成功，处理逻辑与不传一致。 |\n| **P-09** | **重复性校验** | 创建已存在的 `name` (业务规则假设) | 若系统禁止重名，应返回失败提示；若允许，则创建成功。 |\n| **P-10** | **长度边界** | `name` 达到允许的最大长度 (如 50/100 字符) | 创建成功。 |\n| **P-11** | **长度边界** | `name` 超过允许的最大长度 | 返回参数长度超限错误。 |\n| **P-12** | **特殊字符** | `name` 包含 Emoji、HTML 标签等 | 验证系统是否转义存储或拒绝输入。 |\n| **P-13** | **响应数据** | 检查返回的 `data` 对象 | 应包含项目 ID、创建时间等系统自动生成字段。 |\n\n---\n\n### 四、安全与性能测试点 (Security & Performance)\n\n| 编号 | 测试项 | 测试内容 |\n| :--- | :--- | :--- |\n| **S-01** | **敏感信息泄露** | 登录失败响应中，不应返回具体的数据库错误或堆栈信息。 |\n| **S-02** | **传输安全** | 验证接口是否强制使用 HTTPS (生产环境要求)。 |\n| **S-03** | **暴力破解** | 对登录接口进行连续多次错误密码尝试，验证是否有账号锁定或验证码机制。 |\n| **S-04** | **越权访问** | 使用用户 A 的 Token 尝试删除/修改用户 B 的项目 (需结合未展示的接口测试)。 |\n| **S-05** | **接口性能** | 创建项目接口在并发请求下的响应时间 (如 < 500ms)。 |\n| **S-06** | **幂等性** | 快速重复发送相同的"创建项目"请求，验证是否会产生重复数据 (视业务需求而定)。 |\n\n### 五、补充建议\n由于提供的文档内容在\"创建项目\"响应参数处截断，且缺少\"查询项目\"、\"更新项目\"、\"删除项目\"等接口文档，建议补充以下测试工作：\n1.  **完善文档**：获取完整的项目管理接口列表（GET, PUT, DELETE）。\n2.  **关联测试**：测试\"创建项目\"后，立即调用\"查询项目列表\"验证数据是否落库并可查。\n3.  **状态流转**：如果项目有状态（如进行中、已结束），需测试状态变更的接口。\n            \n            \n\"\n            }
          ]
        }
      }
    ]
  },
  \"usage\": {
    \"input_tokens\": 915,
    \"input_tokens_details\": {
      \"text_tokens\": 915
    },
    \"output_tokens\": 3279,
    \"output_tokens_details\": {
      \"reasoning_tokens\": 1447,
      \"text_tokens\": 3279
    },
    \"total_tokens\": 4194
  },
  \"request_id\": \"983491b9-51d1-9924-8372-212bcc55ec86\"
}";
        
        // 测试 parseTestPoints 方法
        List<TestPoint> testPoints = requirementService.parseTestPoints(1L, 1L, "测试需求", llmResult);
        
        // 输出测试结果
        System.out.println("测试点提取结果：");
        System.out.println("提取到的测试点数量：" + testPoints.size());
        
        for (int i = 0; i < testPoints.size(); i++) {
            TestPoint point = testPoints.get(i);
            System.out.println("\n测试点 " + (i + 1) + "：");
            System.out.println("名称：" + point.getName());
            System.out.println("描述：" + point.getDescription());
            System.out.println("风险等级：" + point.getRiskLevel());
            System.out.println("场景类型：" + point.getSceneType());
        }
    }
}