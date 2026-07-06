package com.lg.hq.hqserver.mapper.kr;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface KrMapper {

    @Select("SELECT o.*, oi.product_code, oi.quantity, oi.unit_price " +
            "FROM orders o LEFT JOIN order_items oi ON o.order_no = oi.order_no " +
            "ORDER BY o.created_at DESC LIMIT #{size}")
    List<Map<String, Object>> findRecentOrders(@Param("size") int size);

    // 오늘 주문 수
    @Select("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE()")
    int countTodayOrders();

    // 전체 고객 수
    @Select("SELECT * FROM customer ORDER BY created_at DESC")
    Integer countAllCustomers();

    // 상품 동기화 코드 목록
    @Select("SELECT product_code FROM local_product_price")
    List<String> findSyncedProductCodes();

    // 음수 금액 주문 수
    @Select("SELECT COUNT(*) FROM orders WHERE total_amount < 0")
    int countNegativeOrders();

    @Select("SELECT COUNT(*) FROM orders WHERE status = 'PENDING'")
    int countPendingOrders();

    @Select("SELECT COUNT(*) FROM orders WHERE status = 'SHIPPING'")
    int countShippingOrders();

    // 이메일 누락 고객 수
    @Select("SELECT COUNT(*) FROM customer WHERE contact_email IS NULL OR contact_email = ''")
    int countMissingEmail();

    @Select("SELECT " +
            "    o.*, " +
            "    oi.product_code, " +
            "    oi.quantity, " +
            "    oi.unit_price " +
            "FROM ( " +
            "    SELECT order_no " +
            "    FROM orders " +
            "    WHERE (#{status} = '' OR status = #{status}) " +
            "      AND (#{startDate} = '' OR order_date >= #{startDate}) " +
            "      AND (#{endDate} = '' OR order_date <= #{endDate}) " +
            "      AND (#{keyword} = '' OR order_no LIKE CONCAT(#{keyword}, '%') OR customer_id LIKE CONCAT(#{keyword}, '%')) " +
            "    ORDER BY created_at DESC " +
            "    LIMIT #{size} OFFSET #{offset} " +
            ") p " +
            "JOIN orders o ON p.order_no = o.order_no " +
            "LEFT JOIN order_items oi ON o.order_no = oi.order_no " +
            "ORDER BY o.created_at DESC")
    List<Map<String, Object>> findOrdersPaged(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("size") int size,
            @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM orders " +
            "WHERE (#{status} = '' OR status = #{status}) " +
            "AND (#{keyword} = '' OR order_no LIKE CONCAT('%',#{keyword},'%') " +
            "OR customer_id LIKE CONCAT('%',#{keyword},'%')) " +
            "AND (#{startDate} = '' OR order_date >= #{startDate}) " +
            "AND (#{endDate} = '' OR order_date <= #{endDate})")
    int countOrdersPaged(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    // 상태별 주문 수
    @Select("SELECT COUNT(*) FROM orders WHERE status = #{status}")
    Integer countOrdersByStatus(@Param("status") String status);


}