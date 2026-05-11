package com.testplatform.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LLMLogger {
    private static final String LOG_FILE_PATH = "E:\\test_ai_jccs\\llm\\llm.log";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Lock lock = new ReentrantLock();
    
    public enum LogLevel {
        INFO, ERROR, DEBUG, WARN
    }
    
    public static void info(String module, String operation, String message) {
        log(LogLevel.INFO, module, operation, message, null);
    }
    
    public static void error(String module, String operation, String message, Throwable throwable) {
        log(LogLevel.ERROR, module, operation, message, throwable);
    }
    
    public static void debug(String module, String operation, String message) {
        log(LogLevel.DEBUG, module, operation, message, null);
    }
    
    public static void warn(String module, String operation, String message) {
        log(LogLevel.WARN, module, operation, message, null);
    }
    
    private static void log(LogLevel level, String module, String operation, String message, Throwable throwable) {
        lock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            String logEntry = String.format("[%s] [%s] [%s] [%s] %s",
                timestamp, level.name(), module, operation, message);
            writer.write(logEntry);
            writer.newLine();
            
            if (throwable != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                writer.write(sw.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            // 如果日志写入失败，打印到控制台
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
    
    // 专门用于LLM API调用的日志方法
    public static void logLLMCall(String operation, String prompt, String response, long duration) {
        lock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, true))) {
            String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
            writer.write(String.format("[%s] [INFO] [LLMService] [%s] Started", timestamp, operation));
            writer.newLine();
            writer.write(String.format("[%s] [INFO] [LLMService] [%s] Prompt length: %d characters", timestamp, operation, prompt.length()));
            writer.newLine();
            writer.write(String.format("[%s] [INFO] [LLMService] [%s] Response length: %d characters", timestamp, operation, response.length()));
            writer.newLine();
            writer.write(String.format("[%s] [INFO] [LLMService] [%s] Duration: %d ms", timestamp, operation, duration));
            writer.newLine();
            writer.write(String.format("[%s] [INFO] [LLMService] [%s] Completed", timestamp, operation));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
