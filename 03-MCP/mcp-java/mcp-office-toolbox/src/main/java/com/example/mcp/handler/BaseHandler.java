package com.example.mcp.handler;

import com.example.mcp.handler.file.TxtHandler;
import com.example.mcp.handler.pdf.PdfHandler;
import com.example.mcp.util.AuditLogger;
import com.example.mcp.util.LogUtil;

/**
 * MCP 工具 Handler 抽象基类，提供统一的异常处理、审计日志和耗时统计模板方法。
 * <p>
 * 所有子 Handler 应优先使用 {@link #execute(String, ThrowingSupplier)} 或
 * {@link #executeWithAudit(String, String, ThrowingSupplier)} 包装业务逻辑，
 * 避免重复编写 try-catch-AuditLogger-LogUtil 模板代码。
 * </p>
 *
 * @author Frank Kang
 * @since 2026-07-12
 */
public abstract class BaseHandler {

    /**
     * 执行业务逻辑并统一处理异常，不记录审计日志。
     * <p>
     * 适用于 {@link TxtHandler}、{@link PdfHandler} 等不需要审计日志的场景。
     * 业务异常（IllegalArgumentException、IllegalStateException）返回友好提示，
     * 系统异常返回通用错误信息。
     * </p>
     *
     * @param toolName 工具名称（用于日志标识）
     * @param action   业务逻辑（允许抛出受检异常）
     * @return 业务逻辑的返回结果，或错误消息
     */
    protected String execute(String toolName, ThrowingSupplier<String> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException | IllegalStateException e) {
            LogUtil.warn("{} 参数错误: {}", toolName, e.getMessage());
            return "错误: " + e.getMessage();
        } catch (Exception e) {
            LogUtil.error("{} 系统异常", toolName, e);
            return "系统错误，请稍后重试";
        }
    }

    /**
     * 执行业务逻辑并统一处理异常，同时记录审计日志和耗时统计。
     * <p>
     * 适用于需要审计追踪的场景（如 Excel 读写操作）。
     * 业务异常（IllegalArgumentException、IllegalStateException）返回友好提示，
     * 系统异常返回通用错误信息。
     * </p>
     *
     * @param toolName 工具名称（用于审计日志和日志标识）
     * @param params   参数摘要（用于审计日志）
     * @param action   业务逻辑（允许抛出受检异常）
     * @return 业务逻辑的返回结果，或错误消息
     */
    protected String executeWithAudit(String toolName, String params, ThrowingSupplier<String> action) {
        long start = System.currentTimeMillis();
        try {
            String result = action.get();
            AuditLogger.log(toolName, params, "success", System.currentTimeMillis() - start);
            return result;
        } catch (IllegalArgumentException | IllegalStateException e) {
            long duration = System.currentTimeMillis() - start;
            AuditLogger.log(toolName, params, "error: " + e.getMessage(), duration);
            LogUtil.warn("{} 参数错误: {}", toolName, e.getMessage());
            return "错误: " + e.getMessage();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            AuditLogger.log(toolName, params, "error: " + e.getMessage(), duration);
            LogUtil.error("{} 系统异常", toolName, e);
            return "系统错误，请稍后重试";
        }
    }
}