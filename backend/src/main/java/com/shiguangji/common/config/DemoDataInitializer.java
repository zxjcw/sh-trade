package com.shiguangji.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.mapper.AppUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 仅 dev：把 V3 演示数据里的 {demo} 占位密码编码成 BCrypt(123456)，幂等。
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private static final String PLACEHOLDER = "{demo}";

    private final AppUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        List<AppUser> users = userMapper.selectList(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getPasswordHash, PLACEHOLDER));
        if (users.isEmpty()) {
            return;
        }
        String encoded = passwordEncoder.encode("123456");
        for (AppUser user : users) {
            user.setPasswordHash(encoded);
            userMapper.updateById(user);
        }
        log.info("演示账号密码已初始化为 123456，共 {} 个", users.size());
    }
}
