package com.example.mcp.config;

import com.example.mcp.handler.ExcelHandler;
import com.example.mcp.handler.FetchHandler;
import com.example.mcp.handler.FileSystemHandler;
import com.example.mcp.handler.PictureHandler;
import com.example.mcp.handler.WeatherHandler;
import com.example.mcp.pojo.TextInput;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class McpConfiguration {

    /**
     * 将输入文本转换为大写形式
     *
     * @param input 包含待转换文本的 TextInput对象
     * @return 转换后的大写文本
     */
    public String convertToUpperCase(TextInput input) {
        return input.input()
                .toUpperCase();
    }

    /**
     * 创建一个工具回调函数，用于将输入文本转换为大写形式
     *
     * @return ToolCallback对象，提供文本转大写功能
     */
    @Bean
    public ToolCallback toUpperCase() {
        return FunctionToolCallback.builder("toUpperCase", this::convertToUpperCase)
                .inputType(TextInput.class)
                .description("Put the text to upper case")
                .build();
    }

    /**
     * 创建一个工具回调提供者，用于注册天气服务中的工具方法
     *
     * @param weatherHandler 天气服务实例，包含可作为MCP工具使用的方法
     * @return ToolCallbackProvider对象，提供天气相关的工具功能
     */
    @Bean
    public ToolCallbackProvider weatherTools(WeatherHandler weatherHandler) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(weatherHandler)
                .build();
    }

    @Bean
    public ToolCallbackProvider pictureTools(PictureHandler pictureHandler) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(pictureHandler)
                .build();
    }

    /**
     * 注册文件系统工具（FileSystemHandler），提供文件读取、写入、编辑、搜索、目录浏览等 MCP 工具功能
     *
     * @param fileSystemHandler 文件系统处理器实例
     * @return ToolCallbackProvider 对象，提供文件系统相关的工具功能
     */
    @Bean
    public ToolCallbackProvider fileSystemTools(FileSystemHandler fileSystemHandler) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(fileSystemHandler)
                .build();
    }

    /**
     * 注册 Fetch 工具（FetchHandler），提供 URL 抓取、HTML 转 Markdown 等 MCP 工具功能
     *
     * @param fetchHandler Fetch 处理器实例
     * @return ToolCallbackProvider 对象，提供网页抓取相关的工具功能
     */
    @Bean
    public ToolCallbackProvider fetchTools(FetchHandler fetchHandler) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(fetchHandler)
                .build();
    }

    /**
     * 注册 Excel 工具（ExcelHandler），提供 Excel 读取、写入、格式化、截图等 MCP 工具功能
     *
     * @param excelHandler Excel 处理器实例
     * @return ToolCallbackProvider 对象，提供 Excel 相关的工具功能
     */
    @Bean
    public ToolCallbackProvider excelTools(ExcelHandler excelHandler) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(excelHandler)
                .build();
    }
}