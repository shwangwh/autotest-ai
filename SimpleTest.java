import com.testplatform.service.RequirementService;
import com.testplatform.entity.TestPoint;

import java.util.List;

public class SimpleTest {
    public static void main(String[] args) {
        // 创建 RequirementService 实例
        RequirementService requirementService = new RequirementService(null, null, null);
        
        // 简单的 Markdown 测试文本
        String markdownText = "### 测试点1: 基础路径验证\n验证所有接口请求路径是否均以 /api 开头。\n\n### 测试点2: 响应结构验证\n验证所有接口返回的 JSON 是否包含 success, message, data 字段。\n\n### 测试点3: Content-Type 验证\n验证请求头 Content-Type 是否为 application/json。";
        
        // 测试 parseTestPoints 方法
        List<TestPoint> testPoints = requirementService.parseTestPoints(1L, 1L, "测试需求", markdownText);
        
        // 输出测试结果
        System.out.println("测试点提取结果：");
        System.out.println("提取到的测试点数量：" + testPoints.size());
        
        for (int i = 0; i < testPoints.size(); i++) {
            TestPoint point = testPoints.get(i);
            System.out.println("\n测试点 " + (i + 1) + "：");
            System.out.println("名称：" + point.getName());
            System.out.println("描述：" + point.getDescription());
        }
    }
}