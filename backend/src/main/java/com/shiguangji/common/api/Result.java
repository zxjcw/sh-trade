package com.shiguangji.common.api;

/**
 * 统一响应体：{ code, message, data }
 * code = 0 表示成功；业务错误建议使用 HTTP 200 + code != 0。
 */
public record Result<T>(int code, String message, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "success", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "success", null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
