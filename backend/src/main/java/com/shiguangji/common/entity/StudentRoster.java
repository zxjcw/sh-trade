package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_roster")
public class StudentRoster {

    @TableId(type = IdType.INPUT)
    private String studentNo;
    private String name;
    private String college;
    private String major;
    private String grade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
