package com.shiguangji.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguangji.common.entity.Item;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItemMapper extends BaseMapper<Item> {
}
