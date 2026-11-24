package com.shiguangji.common.security;

/**
 * 登录态：从 JWT 解析出来的当前用户。
 */
public record LoginUser(Long id, String username, String role) {
}
