package com.shiguangji.common.security;

import com.shiguangji.common.api.ErrorCodes;
import com.shiguangji.common.api.Result;
import com.shiguangji.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户快捷入口。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static LoginUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BizException(ErrorCodes.UNAUTHORIZED, "未登录或登录已过期");
        }
        return loginUser;
    }

    public static Long id() {
        return get().id();
    }

    public static String role() {
        return get().role();
    }

    public static boolean isOps() {
        return isOpsOrFalse();
    }

    /**
     * 匿名访问（公开接口）时返回 null，不抛异常。
     */
    public static Long idOrNull() {
        LoginUser loginUser = currentOrNull();
        return loginUser == null ? null : loginUser.id();
    }

    public static boolean isOpsOrFalse() {
        LoginUser loginUser = currentOrNull();
        if (loginUser == null) {
            return false;
        }
        String role = loginUser.role();
        return "operator".equals(role) || "admin".equals(role);
    }

    private static LoginUser currentOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }
}
