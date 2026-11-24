package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exchange_plan")
public class ExchangePlan {

    @TableId(type = IdType.AUTO)
    private Long planId;
    private Integer ringType;
    private Long intentAId;
    private Long intentBId;
    private Long intentCId;
    private String status;
    private LocalDateTime confirmDeadline;
    private LocalDateTime confirmedAAt;
    private LocalDateTime confirmedBAt;
    private LocalDateTime confirmedCAt;
    private LocalDateTime createdAt;
}
