package com.example.mcp.handler;

/**
 * 支持抛出受检异常的函数式接口，用于 {@link BaseHandler#execute(String, ThrowingSupplier)} 等模板方法。
 * <p>
 * 与 {@link java.util.function.Supplier} 的区别在于允许 get() 抛出 {@link Exception}，
 * 使得 Lambda 表达式可以自然调用 {@link java.io.IOException} 等受检异常而不必在内部 try-catch 包装。
 * </p>
 *
 * @param <T> 返回值类型
 * @author Frank Kang
 * @since 2026-07-12
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

    T get() throws Exception;
}