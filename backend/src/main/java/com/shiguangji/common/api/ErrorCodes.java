package com.shiguangji.common.api;

/**
 * 业务错误码。响应体 code 使用这些值，HTTP 状态码见 GlobalExceptionHandler。
 */
public final class ErrorCodes {

    public static final int OK = 0;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int CONFLICT = 409;
    public static final int INTERNAL = 500;

    private ErrorCodes() {
    }
}
