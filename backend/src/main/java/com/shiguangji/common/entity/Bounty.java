package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bounty")
public class Bounty {

    @TableId(type = IdType.AUTO)
    private Long bountyId;
    private Long buyerId;
    private Long categoryId;
    private String keyword;
    private BigDecimal budget;
    private String conditionCode;
    private BigDecimal priceTolerance;
    private String status;
    private Integer matchCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime closedAt;
}
