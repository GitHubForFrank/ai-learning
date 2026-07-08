package com.zmz.sdd.fullstack.core.common;

import lombok.Getter;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §5 + 03-技术方案.md §7.1]
 */
@Getter
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
