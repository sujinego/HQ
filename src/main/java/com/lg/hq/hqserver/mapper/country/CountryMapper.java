package com.lg.hq.hqserver.mapper.country;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


/**
 * KR/US 등 국가 DB에 공통으로 사용되는 Mapper.
 * CountryContext.set("KR") 후 호출하면 KR DB로,
 * CountryContext.set("US") 후 호출하면 US DB로 자동 라우팅.
 */
@Mapper
public interface CountryMapper {

    // ── 주문 ─────────────────────────────────────────
    Integer countTodayOrders();

    // 데이터퀄리티 음수 금액
    Integer countNegativeOrders();

    // 데이터 퀄리티 이메일 누락
    Integer countMissingEmailCustomers();

    // ── 고객 ─────────────────────────────────────────
    Integer countAllCustomers();

    // ── 상품 가격 ─────────────────────────────────────
    List<String> findAllProductCodes();

    // 상태별 주문 수
    Integer countOrdersByStatus(@Param("status") String status);

    List<Map<String, Object>> findRecentOrders(@Param("limit") int limit);

    // 주문 목록 (orders만, JOIN 없음)
    List<Map<String, Object>> findOrdersPaged(
            @Param("keyword")   String keyword,
            @Param("status")    String status,
            @Param("startDate") String startDate,
            @Param("endDate")   String endDate,
            @Param("size")      int size,
            @Param("offset")    int offset);

    // 주문 상세 (order_items)
    List<Map<String, Object>> findOrderItems(@Param("orderNo") String orderNo);

    // 주문 상태 변경
    int updateOrderStatus(@Param("orderNo") String orderNo,
                          @Param("status") String status);

    // ── 주문 삭제 ─────────────────────────────────────────────
    int deleteOrderItems(@Param("orderNo") String orderNo);

    int deleteOrder(@Param("orderNo") String orderNo);

    Map<String, Object> findCustomerCredit(@Param("customerId") String customerId);

    // NOTE: 원본에 @Param이 없어서 파라미터명 매칭이 불안정할 수 있어
    //       XML과의 확실한 매칭을 위해 @Param을 추가했습니다.
    void insertOrder(@Param("orderNo")     String orderNo,
                     @Param("customerId")  String customerId,
                     @Param("totalAmount") double totalAmount,
                     @Param("status")      String status,
                     @Param("orderDate")   String orderDate);

    void insertOrderItem(@Param("orderNo")     String orderNo,
                         @Param("productCode") String productCode,
                         @Param("quantity")    int quantity,
                         @Param("unitPrice")   double unitPrice);

    // 주문 단건 조회 (삭제 전 데이터 확인용)
    Map<String, Object> findOrderByNo(@Param("orderNo") String orderNo);

    // 주문 건수
    int countOrdersPaged(@Param("keyword")   String keyword,
                         @Param("status")    String status,
                         @Param("startDate") String startDate,
                         @Param("endDate")   String endDate);

    // 주문 현재 상태 조회 (변경 전 상태 감사 로그용)
    String selectOrderStatus(@Param("orderNo") String orderNo);
}