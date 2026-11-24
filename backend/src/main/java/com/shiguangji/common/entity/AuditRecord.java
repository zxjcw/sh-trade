package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_record")
public class AuditRecord {

    @TableId(type = IdType.AUTO)
    private Long auditId;
    private Long itemId;
    private Long auditorId;
    private String result;
    private String reason;
    private LocalDateTime createdAt;
}
