package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("credit_record")
public class CreditRecord {

    @TableId(type = IdType.AUTO)
    private Long creditId;
    private Long userId;
    private Integer changeValue;
    private String reason;
    private String orderNo;
    private LocalDateTime createdAt;
}
