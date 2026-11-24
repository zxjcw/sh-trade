package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long reviewId;
    private String orderNo;
    private Long reviewerId;
    private Long targetUserId;
    private Integer rating;
    private String tags;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
