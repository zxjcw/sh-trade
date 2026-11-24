package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("arbitration")
public class Arbitration {

    @TableId(type = IdType.AUTO)
    private Long arbitrationId;
    private Long applicantId;
    private String objectType;
    private String objectId;
    private String arbitrationType;
    private String evidence;
    private String status;
    private String result;
    private Long handlerId;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
