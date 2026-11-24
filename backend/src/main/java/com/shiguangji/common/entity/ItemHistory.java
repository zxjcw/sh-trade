package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("item_history")
public class ItemHistory {

    @TableId(type = IdType.AUTO)
    private Long historyId;
    private Long itemId;
    private String eventType;
    private LocalDateTime eventTime;
    private String description;
    private Long relatedUserId;
    private String orderNo;
    private String message;
}
