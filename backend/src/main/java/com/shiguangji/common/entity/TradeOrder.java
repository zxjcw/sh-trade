package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("trade_order")
public class TradeOrder {

    @TableId(type = IdType.INPUT)
    private String orderNo;
    private String orderType;
    private Long buyerId;
    private Long sellerId;
    private Long itemId;
    private BigDecimal amount;
    private String deliveryMethod;
    private Long pickupPointId;
    private String tradeStatus;
    private String afterSaleStatus;
    private Long sourceBountyId;
    private Long planId;
    private LocalDateTime payDeadline;
    private LocalDateTime confirmDeadline;
    private LocalDateTime paidAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
