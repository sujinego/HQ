package com.lg.hq.hqserver.mapper.country;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * KR/US 등 국가 DB에 공통으로 사용되는 Mapper.
 * CountryContext.set("KR") 후 호출하면 KR DB로,
 * CountryContext.set("US") 후 호출하면 US DB로 자동 라우팅.
 */
@Mapper
public interface CountryMapper {

    // ── 주문 ─────────────────────────────────────────
    @Select("SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE()")
    Integer countTodayOrders();

    // 데이터퀄리티 음수 금액
    @Select("SELECT COUNT(*) FROM orders WHERE total_amount < 0")
    Integer countNegativeOrders();

    //  데이터 퀄리티 이메일 누락
    @Select("SELECT COUNT(*) FROM customer " +
            "WHERE contact_email IS NULL OR contact_email = ''")
    Integer countMissingEmailCustomers();

    // ── 고객 ─────────────────────────────────────────
    @Select("SELECT COUNT(*) FROM customer")
    Integer countAllCustomers();


    // ── 상품 가격 ─────────────────────────────────────
    @Select("SELECT product_code FROM local_product_price")
    List<String> findAllProductCodes();

    // 상태별 주문 수
    @Select("SELECT COUNT(*) FROM orders WHERE status = #{status}")
    Integer countOrdersByStatus(@Param("status") String status);

    @Select("SELECT o.*, oi.product_code, oi.quantity, oi.unit_price " +
            "FROM orders o " +
            "LEFT JOIN order_items oi ON o.order_no = oi.order_no " +
            "ORDER BY o.created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecentOrders(@Param("limit") int limit);

    // 주문 목록 (orders만, JOIN 없음)
    @Select("<script>" +
            "SELECT order_no, customer_id, total_amount, status, order_date, created_at " +
            "FROM orders WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (order_no LIKE CONCAT('%',#{keyword},'%') " +
            "OR customer_id LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='startDate != null and startDate != \"\"'>" +
            "AND order_date &gt;= #{startDate} " +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            "AND order_date &lt;= #{endDate} " +
            "</if>" +
            "ORDER BY created_at DESC " +
            "LIMIT #{size} OFFSET #{offset}" +
            "</script>")
    List<Map<String, Object>> findOrdersPaged(
            @Param("keyword")   String keyword,
            @Param("status")    String status,
            @Param("startDate") String startDate,
            @Param("endDate")   String endDate,
            @Param("size")      int size,
            @Param("offset")    int offset);

    // 주문 상세 (order_items)
    @Select("SELECT oi.product_code, oi.quantity, oi.unit_price, " +
            "oi.quantity * oi.unit_price AS subtotal " +
            "FROM order_items oi " +
            "WHERE oi.order_no = #{orderNo} " +
            "ORDER BY oi.id")
    List<Map<String, Object>> findOrderItems(@Param("orderNo") String orderNo);

    // 주문 상태 변경
    @Update("UPDATE orders SET status = #{status} WHERE order_no = #{orderNo}")
    int updateOrderStatus(@Param("orderNo") String orderNo,
                          @Param("status") String status);

    // ── 주문 삭제 ─────────────────────────────────────────────
    @Delete("DELETE FROM order_items WHERE order_no = #{orderNo}")
    int deleteOrderItems(@Param("orderNo") String orderNo);

    @Delete("DELETE FROM orders WHERE order_no = #{orderNo}")
    int deleteOrder(@Param("orderNo") String orderNo);

    @Select("SELECT credit_limit, current_credit FROM customer " +
            "WHERE customer_id = #{customerId}")
    Map<String, Object> findCustomerCredit(@Param("customerId") String customerId);


    @Insert("INSERT INTO orders " +
            "(order_no, customer_id, total_amount, status, order_date, created_at) " +
            "VALUES (#{orderNo}, #{customerId}, #{totalAmount}, " +
            "#{status}, #{orderDate}, NOW())")
    void insertOrder(String orderNo, String customerId, double totalAmount, String status, String orderDate);

    @Insert("INSERT INTO order_items " +
            "(order_no, product_code, quantity, unit_price) " +
            "VALUES (#{orderNo}, #{productCode}, #{quantity}, #{unitPrice})")
    void insertOrderItem(String orderNo, String productCode, int quantity, double unitPrice);

    // 주문 단건 조회 (삭제 전 데이터 확인용)
    @Select("SELECT order_no, customer_id, total_amount, status, order_date " +
            "FROM orders WHERE order_no = #{orderNo}")
    Map<String, Object> findOrderByNo(@Param("orderNo") String orderNo);

    // 주문 건수
    @Select("<script>" +
            "SELECT COUNT(*) FROM orders WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (order_no LIKE CONCAT('%',#{keyword},'%') " +
            "OR customer_id LIKE CONCAT('%',#{keyword},'%')) " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='startDate != null and startDate != \"\"'>" +
            "AND order_date &gt;= #{startDate} " +
            "</if>" +
            "<if test='endDate != null and endDate != \"\"'>" +
            "AND order_date &lt;= #{endDate} " +
            "</if>" +
            "</script>")
    int countOrdersPaged(@Param("keyword")   String keyword,
                         @Param("status")    String status,
                         @Param("startDate") String startDate,
                         @Param("endDate")   String endDate);

    // 주문 현재 상태 조회 (변경 전 상태 감사 로그용)
    @Select("SELECT status FROM orders WHERE order_no = #{orderNo}")
    String selectOrderStatus(@Param("orderNo") String orderNo);
}