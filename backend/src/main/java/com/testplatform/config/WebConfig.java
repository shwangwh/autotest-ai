package com.testplatform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源配置 - 将 allure-reports 目录映射为 HTTP 可访问路径
 * 访问地址: http://localhost:8080/allure-reports/{taskId}/index.html
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取 allure-reports 的绝对路径
        Path reportsPath = Paths.get("allure-reports").toAbsolutePath();

        registry.addResourceHandler("/allure-reports/**")
                .addResourceLocations("file:" + reportsPath.toString() + "/");
    }
}
