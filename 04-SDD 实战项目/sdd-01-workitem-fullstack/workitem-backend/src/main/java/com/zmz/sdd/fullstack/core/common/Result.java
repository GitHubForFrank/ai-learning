package com.zmz.sdd.fullstack.core.common;

import lombok.Data;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §1.1] 统一响应封装
 */
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = ErrorCode.SUCCESS.getCode();
        r.message = ErrorCode.SUCCESS.getDefaultMessage();
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        Result<T> r = new Result<>();
        r.code = errorCode.getCode();
        r.message = message != null ? message : errorCode.getDefaultMessage();
        r.data = null;
        r.timestamp = System.currentTimeMillis();
        return r;
    }
}
