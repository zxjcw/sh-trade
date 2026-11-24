package com.shiguangji.modules.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiguangji.common.dto.Dtos.UserDto;
import com.shiguangji.common.entity.AppUser;
import com.shiguangji.common.entity.StudentRoster;
import com.shiguangji.common.entity.VerificationRecord;
import com.shiguangji.common.exception.BizException;
import com.shiguangji.common.mapper.AppUserMapper;
import com.shiguangji.common.mapper.StudentRosterMapper;
import com.shiguangji.common.mapper.VerificationRecordMapper;
import com.shiguangji.modules.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final AppUserMapper userMapper;
    private final StudentRosterMapper rosterMapper;
    private final VerificationRecordMapper recordMapper;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional
    public UserDto submit(String studentNo, String name, String college, String major, String grade) {
        AppUser user = userService.current();
        if ("verified".equals(user.getVerifyStatus())) {
            throw BizException.conflict("你已完成学生认证，无需重复提交");
        }
        if (studentNo == null || studentNo.isBlank()) {
            throw BizException.badRequest("请填写学号");
        }
        String no = studentNo.trim();
        StudentRoster roster = rosterMapper.selectById(no);
        if (roster == null) {
            record(user.getUserId(), no, name, college, major, grade, "rejected", "学籍库中未找到该学号");
            throw BizException.badRequest("学籍库中未找到该学号，请核对后重试");
        }
        if (name == null || !name.trim().equals(roster.getName())) {
            record(user.getUserId(), no, name, college, major, grade, "rejected", "姓名与学籍信息不一致");
            throw BizException.badRequest("姓名与学籍信息不一致");
        }
        if (college != null && !college.isBlank() && !college.trim().equals(roster.getCollege())) {
            record(user.getUserId(), no, name, college, major, grade, "rejected", "学院与学籍信息不一致");
            throw BizException.badRequest("学院与学籍信息不一致");
        }
        Long bound = userMapper.selectCount(new LambdaQueryWrapper<AppUser>()
                .eq(AppUser::getStudentNo, no)
                .ne(AppUser::getUserId, user.getUserId()));
        if (bound != null && bound > 0) {
            record(user.getUserId(), no, name, college, major, grade, "rejected", "该学号已被其他账号绑定");
            throw BizException.conflict("该学号已被其他账号绑定");
        }
        record(user.getUserId(), no, name, college, major, grade, "approved", null);

        user.setStudentNo(no);
        user.setVerifyStatus("verified");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        notificationService.send(user.getUserId(), "system", "user", user.getUserId(),
                "学生认证已通过，现在可以发布物品和发起交易了。", null);
        return userService.toDto(user);
    }

    private void record(Long userId, String studentNo, String name, String college, String major, String grade,
                        String result, String rejectReason) {
        VerificationRecord r = new VerificationRecord();
        r.setUserId(userId);
        r.setMethod("student_no");
        r.setSubmittedStudentNo(studentNo);
        r.setSubmittedName(name);
        r.setSubmittedCollege(college);
        r.setSubmittedMajor(major);
        r.setSubmittedGrade(grade);
        r.setResult(result);
        r.setRejectReason(rejectReason);
        r.setAppliedAt(LocalDateTime.now());
        if ("approved".equals(result)) {
            r.setVerifiedAt(LocalDateTime.now());
        }
        recordMapper.insert(r);
    }
}
