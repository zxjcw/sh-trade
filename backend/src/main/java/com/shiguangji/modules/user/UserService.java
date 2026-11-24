package com.shiguangji.modules.user;

import com.shiguangji.common.dto.Dtos.UserDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.StudentRoster;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.StudentRosterMapper;
import com.shiguangji.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户读取与校验的公共入口。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserMapper userMapper;
    private final StudentRosterMapper rosterMapper;

    public AppUser get(Long id) {
        AppUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }

    public AppUser current() {
        return get(CurrentUser.id());
    }

    public AppUser requireVerified(AppUser user) {
        if (!"verified".equals(user.getVerifyStatus())) {
            throw BizException.forbidden("请先完成学生认证");
        }
        return user;
    }

    public void requireNotBanned(AppUser user) {
        if (Boolean.TRUE.equals(user.getBanned())) {
            throw BizException.forbidden("账号已被封禁，如有疑问请联系管理员");
        }
    }

    public StudentRoster rosterOf(AppUser user) {
        if (user.getStudentNo() == null) {
            return null;
        }
        return rosterMapper.selectById(user.getStudentNo());
    }

    public UserDto toDto(AppUser user) {
        StudentRoster roster = rosterOf(user);
        return new UserDto(
                user.getUserId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole(),
                user.getVerifyStatus(),
                user.getCreditScore(),
                user.getBalance(),
                user.getStudentNo(),
                roster == null ? null : roster.getCollege(),
                roster == null ? null : roster.getMajor(),
                roster == null ? null : roster.getGrade(),
                user.getContact(),
                Boolean.TRUE.equals(user.getBanned()),
                user.getCreatedAt());
    }
}
