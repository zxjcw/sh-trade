package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pickup_point")
public class PickupPoint {

    @TableId(type = IdType.AUTO)
    private Long pickupPointId;
    private String name;
    private String address;
    private String coordinate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
