package com.gitgui.core.exception;

/**
 * 应用统一异常
 * <p>所有服务层异常统一抛出本异常，携带 {@link ErrorCode} 与中文 message。</p>
 * <p>由 {@code GlobalExceptionHandler} 转换为 UI 友好提示。</p>
 *
 * @author FrankKang
 * @since 2026-05-27
 */
public class GitGuiException extends RuntimeException {

    /** 错误码 */
    private final ErrorCode errorCode;

    /**
     * 构造异常。
     *
     * @param errorCode 错误码
     * @param message   中文错误详情（覆盖 errorCode 默认提示）
     */
    public GitGuiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 构造异常（使用错误码默认提示）。
     *
     * @param errorCode 错误码
     */
    public GitGuiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 构造异常并携带原始 cause。
     *
     * @param errorCode 错误码
     * @param message   中文错误详情
     * @param cause     原始异常
     */
    public GitGuiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 构造异常（携带 cause，使用错误码默认提示）。
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public GitGuiException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
