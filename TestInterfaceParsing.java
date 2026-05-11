import com.testplatform.service.RequirementService;
import com.testplatform.entity.TestPoint;

import java.util.List;

public class TestInterfaceParsing {
    public static void main(String[] args) {
        // 创建 RequirementService 实例
        RequirementService requirementService = new RequirementService(null, null, null);
        
        // 模拟用户指定的文档格式
        String llmResult = "### 1.1 用户登录\n\n**接口说明**: 用户登录获取认证令牌\n\n| 项目 | 说明 |\n|------|------|\n| 接口名称 | 用户登录 |\n| 请求方式 | POST |\n| 请求路径 | `/api/auth/login` |\n| Content-Type | `application/json` |\n| 是否需要认证 | 否 |\n\n#### 请求参数\n\n| 参数名 | 类型 | 必填 | 说明 |\n|--------|------|------|------|\n| username | String | 是 | 用户账号 |\n| password | String | 是 | 用户密码 |\n\n#### 请求示例\n\n```json\n{\n  \"username\": \"admin\",\n  \"password\": \"123456\"\n}\n```\n\n#### 成功响应\n\n| 参数名 | 类型 | 说明 |\n|--------|------|------|\n| success | Boolean | 登录是否成功 |\n| message | String | 响应消息 |\n| authorization | String | Basic认证令牌 |\n| username | String | 用户名 |\n\n#### 成功响应示例\n\n```json\n{\n  \"success\": true,\n  \"message\": \"Login successful.\",\n  \"authorization\": \"Basic YWRtaW46MTIzNDU2\",\n  \"username\": \"admin\"\n}\n```\n\n#### 失败响应示例\n\n```json\n{\n  \"success\": false,\n  \"message\": \"Invalid username or password.\"\n}\n```";
        
        // 测试 parseTestPoints 方法
        List<TestPoint> testPoints = requirementService.parseTestPoints(1L, 1L, "测试需求", llmResult);
        
        // 输出测试结果
        System.out.println("测试点提取结果：");
        System.out.println("提取到的测试点数量：" + testPoints.size());
        
        for (int i = 0; i < testPoints.size(); i++) {
            TestPoint point = testPoints.get(i);
            System.out.println("\n测试点 " + (i + 1) + "：");
            System.out.println("测试点名称：" + point.getPointName());
            System.out.println("名称：" + point.getName());
            System.out.println("描述：" + point.getDescription());
            System.out.println("场景类型：" + point.getSceneType());
            System.out.println("风险等级：" + point.getRiskLevel());
        }
    }
}