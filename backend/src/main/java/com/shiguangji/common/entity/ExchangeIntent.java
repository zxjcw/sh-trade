package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exchange_intent")
public class ExchangeIntent {

    @TableId(type = IdType.AUTO)
    private Long intentId;
    private Long userId;
    private Long myItemId;
    private String matchType;
    private Long targetItemId;
    private Long targetCategoryId;
    private String keyword;
    private String status;
    private LocalDateTime createdAt;
}
