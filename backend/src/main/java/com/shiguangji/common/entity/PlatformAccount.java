package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("platform_account")
public class PlatformAccount {

    @TableId(type = IdType.INPUT)
    private Long accountNo;
    private BigDecimal escrowBalance;
    private BigDecimal totalInflow;
    private BigDecimal totalOutflow;
    private LocalDateTime updatedAt;
}
