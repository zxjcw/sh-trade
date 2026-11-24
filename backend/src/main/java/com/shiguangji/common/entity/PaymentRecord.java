package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long paymentId;
    private String orderNo;
    private String accountType;
    private Long userId;
    private Long accountIdentifier;
    private String paymentType;
    private BigDecimal amount;
    private BigDecimal balanceSnapshot;
    private String status;
    private String paymentMethod;
    private String externalNo;
    private String remark;
    private LocalDateTime createdAt;
}
