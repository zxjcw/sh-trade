package com.shiguangji.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("item_image")
public class ItemImage {

    @TableId(type = IdType.AUTO)
    private Long imageId;
    private Long itemId;
    private String path;
    private Integer sortOrder;
    private Boolean isCover;
    private String status;
}
