package com.shiguangji.common.exception;

import com.shiguangji.common.api.ErrorCodes;

/**
 * 业务异常：code 与接口文档错误码一致。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(ErrorCodes.BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static BizException badRequest(String message) {
        return new BizException(ErrorCodes.BAD_REQUEST, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(ErrorCodes.UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(ErrorCodes.FORBIDDEN, message);
    }

    public static BizException notFound(String message) {
        return new BizException(ErrorCodes.NOT_FOUND, message);
    }

    public static BizException conflict(String message) {
        return new BizException(ErrorCodes.CONFLICT, message);
    }
}
