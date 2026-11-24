package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("item")
public class Item {

    @TableId(type = IdType.AUTO)
    private Long itemId;
    private String title;
    private Long categoryId;
    private Long currentHolderId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal purchasePrice;
    private String conditionCode;
    private String description;
    private String status;
    private String source;
    private String notesTag;
    private LocalDateTime listedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
