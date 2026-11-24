package com.shiguangji.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.LoginResult;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.security.LoginUser;
import com.shiguangji.common.security.JwtService;
import com.shiguangji.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;

    public LoginResult login(String username, String password) {
        AppUser user = userMapper.selectOne(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BizException.badRequest("用户名或密码错误");
        }
        userService.requireNotBanned(user);
        return new LoginResult(tokenOf(user), userService.toDto(user));
    }

    @Transactional
    public LoginResult register(String username, String password, String nickname, String contact) {
        String name = username == null ? "" : username.trim();
        if (name.length() < 3 || name.length() > 20) {
            throw BizException.badRequest("用户名长度需在 3~20 个字符之间");
        }
        if (password == null || password.length() < 6) {
            throw BizException.badRequest("密码至少 6 位");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getUsername, name));
        if (exists != null && exists > 0) {
            throw BizException.conflict("用户名已被占用");
        }
        AppUser user = new AppUser();
        user.setUsername(name);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname == null || nickname.isBlank() ? name : nickname.trim());
        user.setContact(contact == null || contact.isBlank() ? null : contact.trim());
        user.setRole("student");
        user.setVerifyStatus("unverified");
        user.setCreditScore(100);
        user.setBanned(false);
        user.setBalance(BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return new LoginResult(tokenOf(user), userService.toDto(user));
    }

    private String tokenOf(AppUser user) {
        return jwtService.issue(new LoginUser(user.getUserId(), user.getUsername(), user.getRole()));
    }
}
