import com.testplatform.service.RequirementService;
import com.testplatform.entity.TestPoint;

import java.lang.reflect.Method;
import java.util.List;

public class ReflectionTest {
    public static void main(String[] args) throws Exception {
        // 创建 RequirementService 实例
        RequirementService requirementService = new RequirementService(null, null, null);
        
        // 测试 extractLlmText 方法
        testExtractLlmText(requirementService);
        
        // 测试 parseTestPointsFromMarkdown 方法
        testParseTestPointsFromMarkdown(requirementService);
    }
    
    private static void testExtractLlmText(RequirementService service) throws Exception {
        // 获取 extractLlmText 方法
        Method extractLlmTextMethod = RequirementService.class.getDeclaredMethod("extractLlmText", String.class);
        extractLlmTextMethod.setAccessible(true);
        
        // 模拟 LLM 返回的 JSON 格式
        String llmResult = "{
  \"output\": {
    \"choices\": [
      {
        \"finish_reason\": \"stop\",
        \"message\": {
          \"content\": [
            {
              \"text\": \"### 测试点1: 基础路径验证\n验证所有接口请求路径是否均以 /api 开头。\n\n### 测试点2: 响应结构验证\n验证所有接口返回的 JSON 是否包含 success, message, data 字段。\n\n### 测试点3: Content-Type 验证\n验证请求头 Content-Type 是否为 application/json。\"\n            }
          ]
        }
      }
    ]
  }
}";
        
        // 调用方法
        String result = (String) extractLlmTextMethod.invoke(service, llmResult);
        System.out.println("extractLlmText 测试结果：");
        System.out.println(result);
        System.out.println("\n");
    }
    
    private static void testParseTestPointsFromMarkdown(RequirementService service) throws Exception {
        // 获取 parseTestPointsFromMarkdown 方法
        Method parseMethod = RequirementService.class.getDeclaredMethod("parseTestPointsFromMarkdown", Long.class, Long.class, String.class, String.class);
        parseMethod.setAccessible(true);
        
        // 测试 Markdown 文本
        String markdownText = "### 测试点1: 基础路径验证\n验证所有接口请求路径是否均以 /api 开头。\n\n### 测试点2: 响应结构验证\n验证所有接口返回的 JSON 是否包含 success, message, data 字段。\n\n### 测试点3: Content-Type 验证\n验证请求头 Content-Type 是否为 application/json。";
        
        // 调用方法
        List<TestPoint> testPoints = (List<TestPoint>) parseMethod.invoke(service, 1L, 1L, "测试需求", markdownText);
        
        // 输出结果
        System.out.println("parseTestPointsFromMarkdown 测试结果：");
        System.out.println("提取到的测试点数量：" + testPoints.size());
        
        for (int i = 0; i < testPoints.size(); i++) {
            TestPoint point = testPoints.get(i);
            System.out.println("\n测试点 " + (i + 1) + "：");
            System.out.println("名称：" + point.getName());
            System.out.println("描述：" + point.getDescription());
        }
    }
}