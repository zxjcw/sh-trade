package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verification_record")
public class VerificationRecord {

    @TableId(type = IdType.AUTO)
    private Long verificationId;
    private Long userId;
    private String method;
    private String submittedStudentNo;
    private String submittedName;
    private String submittedCollege;
    private String submittedMajor;
    private String submittedGrade;
    private String result;
    private String rejectReason;
    private LocalDateTime appliedAt;
    private LocalDateTime verifiedAt;
}
