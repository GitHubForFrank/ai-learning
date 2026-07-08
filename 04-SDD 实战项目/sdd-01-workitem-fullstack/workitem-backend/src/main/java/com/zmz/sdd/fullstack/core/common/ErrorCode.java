package com.zmz.sdd.fullstack.core.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §5 + conventions/api-versioning.md §6.1]
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, HttpStatus.OK, "success"),

    // 1xxx 客户端 / 参数
    PARAM_INVALID(1001, HttpStatus.BAD_REQUEST, "Parameter invalid"),
    WORKITEM_NOT_FOUND(1002, HttpStatus.NOT_FOUND, "Workitem not found"),
    WORKITEM_DONE_IMMUTABLE(1003, HttpStatus.CONFLICT, "Done workitem cannot change priority or dueDate"),

    // 3xxx 鉴权(Task001)
    AUTH_INVALID_CREDENTIAL(3001, HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    AUTH_LOCKED(3002, HttpStatus.LOCKED, "Account locked. Try again later"),
    AUTH_REQUIRED(3003, HttpStatus.UNAUTHORIZED, "Authentication required"),

    // 9xxx
    INTERNAL_ERROR(9999, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
