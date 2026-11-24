package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("delivery_record")
public class DeliveryRecord {

    @TableId(type = IdType.AUTO)
    private Long deliveryId;
    private String orderNo;
    private String method;
    private LocalDateTime agreedTime;
    private String meetupPlace;
    private String pickupCode;
    private LocalDateTime verifyTime;
    private LocalDateTime inboundTime;
    private LocalDateTime outboundTime;
    private Long pickupPointId;
}
