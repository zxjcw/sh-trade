package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {

    @TableId(type = IdType.AUTO)
    private Long userId;
    private String username;
    private String passwordHash;
    private String nickname;
    private String studentNo;
    private String role;
    private String verifyStatus;
    private Integer creditScore;
    private Boolean banned;
    private LocalDateTime bannedUntil;
    private String contact;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
