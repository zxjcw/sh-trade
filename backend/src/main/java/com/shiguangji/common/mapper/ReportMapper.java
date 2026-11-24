package com.shiguangji.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 运营报表：实时 SQL 聚合，不落表。
 */
@Mapper
public interface ReportMapper {

    @Select("SELECT COUNT(*) FROM trade_order WHERE trade_status = 'completed'")
    long completedCount();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM trade_order WHERE trade_status = 'completed'")
    BigDecimal completedAmount();

    @Select("SELECT COUNT(*) FROM trade_order")
    long totalOrders();

    @Select("SELECT COUNT(*) FROM trade_order WHERE after_sale_status = 'refunded'")
    long refundedOrders();

    @Select("SELECT COUNT(*) FROM item WHERE status = 'pending'")
    long pendingAudits();

    @Select("SELECT COUNT(*) FROM arbitration WHERE status = 'pending'")
    long pendingArbitrations();

    @Select("SELECT COUNT(*) FROM app_user WHERE verify_status = 'verified'")
    long verifiedUsers();

    @Select("SELECT COUNT(*) FROM item WHERE status = 'on_sale'")
    long onSaleItems();

    @Select("""
            SELECT c.name AS name, COUNT(*) AS cnt, COALESCE(SUM(o.amount), 0) AS amount
            FROM trade_order o
            JOIN item i ON i.item_id = o.item_id
            JOIN category c ON c.category_id = i.category_id
            WHERE o.trade_status = 'completed'
            GROUP BY c.name
            ORDER BY cnt DESC
            """)
    List<Map<String, Object>> categoryStats();

    @Select("""
            SELECT DATE_FORMAT(completed_at, '%Y-%m') AS month, COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS amount
            FROM trade_order
            WHERE trade_status = 'completed' AND completed_at IS NOT NULL
            GROUP BY month
            ORDER BY month
            """)
    List<Map<String, Object>> monthlyStats();
}
