package com.shiguangji.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiguangji.common.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    @Select("SELECT COUNT(*) FROM trade_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    int countByOrderNoPrefix(@Param("prefix") String prefix);

    /**
     * 当前前缀下已用的最大流水号（订单号形如 SO20251112001，前缀长度 10）。
     * 用 MAX 而不是 COUNT，避免出现空洞或并发下单时的重号。
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(order_no, LENGTH(#{prefix}) + 1) AS UNSIGNED)), 0) "
            + "FROM trade_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    int maxSequence(@Param("prefix") String prefix);
}
