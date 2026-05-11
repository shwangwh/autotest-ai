package com.testplatform.util;

public class LLMLoggerTest {
    public static void main(String[] args) {
        // 测试不同级别的日志
        LLMLogger.info("Test", "testInfo", "这是一条信息日志");
        LLMLogger.warn("Test", "testWarn", "这是一条警告日志");
        LLMLogger.error("Test", "testError", "这是一条错误日志", new Exception("测试异常"));
        LLMLogger.debug("Test", "testDebug", "这是一条调试日志");
        
        // 测试LLM API调用日志
        LLMLogger.logLLMCall("testLLMCall", "测试提示词", "测试响应", 1234);
        
        System.out.println("日志测试完成，请查看 E:\\test_ai_jccs\\llm\\llm.log 文件");
    }
}
