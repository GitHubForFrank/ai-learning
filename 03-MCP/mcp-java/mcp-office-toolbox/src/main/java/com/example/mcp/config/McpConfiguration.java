package com.example.mcp.config;

import com.example.mcp.handler.BaseHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

/**
 * MCP 工具配置类，自动扫描所有带有 @Service 注解的 BaseHandler 子类并注册为 Spring AI 工具。
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
@Configuration
public class McpConfiguration {

    /**
     * 自动扫描所有 Handler Bean 并注册为统一的 ToolCallbackProvider。
     * 通过 ApplicationContext 获取所有 @Service Bean，筛选出 BaseHandler 子类实例，
     * 批量注册到 MethodToolCallbackProvider 中。
     *
     * @param applicationContext Spring 应用上下文
     * @return ToolCallbackProvider 对象，包含所有已注册的 MCP 工具功能
     */
    @Bean
    public ToolCallbackProvider allTools(ApplicationContext applicationContext) {
        Map<String, Object> handlers = applicationContext.getBeansWithAnnotation(Service.class);
        List<BaseHandler> handlerObjects = new ArrayList<>();
        for (Map.Entry<String, Object> entry : handlers.entrySet()) {
            if (entry.getValue() instanceof BaseHandler handler) {
                handlerObjects.add(handler);
            }
        }
        return MethodToolCallbackProvider.builder()
                                         .toolObjects(handlerObjects.toArray())
                                         .build();
    }
}