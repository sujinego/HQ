package com.lg.hq.hqserver.service;

import com.lg.hq.hqserver.common.ApiResponse;
import com.lg.hq.hqserver.common.SecurityUtils;
import com.lg.hq.hqserver.config.CountryContext;
import com.lg.hq.hqserver.mapper.country.CountryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lg.hq.hqserver.common.SecurityUtils.getCurrentUsername;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);


    private final CountryMapper countryMapper;
    private final AuditLogService auditLogService;

    public OrderService(
            CountryMapper countryMapper,
            AuditLogService auditLogService,
            CacheService cacheService) {
        this.countryMapper = countryMapper;
        this.auditLogService = auditLogService;
    }

    // ══════════════════════════════════════════════════
    // 읽기 - Controller: 모든 Role 허용
    // Service: 전국가 조회, 권한 체크 없음
    // ══════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getOrders(String country,
                                         String keyword,
                                         String status,
                                         String startDate,
                                         String endDate,
                                         int page, int size) {
        // 국가 지정 시 해당 국가만
        if (country != null && !country.isEmpty()) {
            return getOrdersByCountry(
                    country, keyword, status, startDate, endDate, page, size);
        }
        // 전국가 합산
        return getAllOrders(keyword, status, startDate, endDate, page, size);
    }

    private Map<String, Object> getOrdersByCountry(String country,
                                                   String keyword,
                                                   String status,
                                                   String startDate,
                                                   String endDate,
                                                   int page, int size) {
        try {
            CountryContext.set(country);
            int total = countryMapper.countOrdersPaged(
                    keyword, status, startDate, endDate);
            List<Map<String, Object>> list = countryMapper.findOrdersPaged(
                    keyword, status, startDate, endDate, size, (page - 1) * size);
            list.forEach(o -> o.put("_country", country));
            return buildPageResult(list, total, page, size);
        } finally {
            CountryContext.clear();
        }
    }

    private Map<String, Object> getAllOrders(String keyword,
                                             String status,
                                             String startDate,
                                             String endDate,
                                             int page, int size) {
        List<Map<String, Object>> allList = new ArrayList<>();
        int totalCount = 0;

        for (String code : List.of("KR", "US")) {
            try {
                CountryContext.set(code);
                totalCount += countryMapper.countOrdersPaged(
                        keyword, status, startDate, endDate);
                List<Map<String, Object>> list = countryMapper.findOrdersPaged(
                        keyword, status, startDate, endDate, size, 0);
                list.forEach(o -> o.put("_country", code));
                allList.addAll(list);
            } finally {
                CountryContext.clear();
            }
        }

        allList.sort((a, b) ->
                String.valueOf(b.getOrDefault("created_at", ""))
                        .compareTo(String.valueOf(a.getOrDefault("created_at", ""))));

        int from = (page - 1) * size;
        int to   = Math.min(from + size, allList.size());
        List<Map<String, Object>> paged = from < allList.size()
                ? allList.subList(from, to) : new ArrayList<>();

        return buildPageResult(paged, totalCount, page, size);
    }


    /**
     * 주문 등록 트랜잭션
     * 1. 주문 저장
     * 2. 신용한도 체크
     * 3. fact_sales 반영
     * 실패 시 전체 롤백
     */
    // ══════════════════════════════════════════════════
    // 쓰기 (KR 고정) - KR_USER, ADMIN만 호출 가능
    // ══════════════════════════════════════════════════


    @Transactional(transactionManager = "krTransactionManager")
    public ApiResponse<Void> createOrder(Map<String, Object> req, String clientIp) {
        String orderNo     = (String) req.get("orderNo");
        String customerId  = (String) req.get("customerId");
        String productCode = (String) req.get("productCode");
        int    quantity    = req.get("quantity") != null
                ? ((Number) req.get("quantity")).intValue() : 1;
        double unitPrice   = req.get("unitPrice") != null
                ? ((Number) req.get("unitPrice")).doubleValue() : 0;
        double totalAmount = req.get("totalAmount") != null
                ? ((Number) req.get("totalAmount")).doubleValue()
                : quantity * unitPrice;
        String orderDate   = (String) req.get("orderDate");
        String status      = req.get("status") != null
                ? (String) req.get("status") : "PENDING";

        try {
            CountryContext.set("KR");

            // 신용한도 체크
            checkCreditLimit(customerId, totalAmount);

            // 주문 등록
            countryMapper.insertOrder(
                    orderNo, customerId, totalAmount, status, orderDate);

            if (productCode != null && !productCode.isEmpty()) {
                countryMapper.insertOrderItem(
                        orderNo, productCode, quantity, unitPrice);
            }

            // 감사 로그
            auditLogService.logCreate(
                    "ORDER", orderNo,
                    SecurityUtils.getCurrentUsername(), clientIp);

            log.info("[OrderService] 주문 등록 - {} (by:{} ip:{})",
                    orderNo, SecurityUtils.getCurrentUsername(), clientIp);

            return ApiResponse.success("주문이 등록되었습니다.");

        } catch (IllegalArgumentException e) {
            // 신용한도 초과는 비즈니스 예외 → 400
            log.warn("[OrderService] 신용한도 초과 - {}", e.getMessage());
            return ApiResponse.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("[OrderService] 주문 등록 실패: {}", e.getMessage());
            return ApiResponse.error("주문 등록 실패: " + e.getMessage());
        } finally {
            CountryContext.clear();
        }
    }

    /**
     * 주문 상태 변경 트랜잭션
     *
     * @return
     */
    @Transactional("krTransactionManager")
    public ApiResponse<Void> updateOrderStatus(String orderNo, String newStatus, String clientIp) {

        try {
            CountryContext.set("KR");
            // 변경 전 상태 조회
            String before = countryMapper.selectOrderStatus(orderNo);
            int updated = countryMapper.updateOrderStatus(orderNo, newStatus);

            if (updated == 0) {
                return ApiResponse.error(400, "주문을 찾을 수 없습니다: " + orderNo);
            }

            // 감사 로그 (누가 바꿨는지 포함)
            auditLogService.logUpdate(
                    "ORDER", orderNo,
                    "status=" + before,
                    "status=" + newStatus,
                    SecurityUtils.getCurrentUsername(),  // ← 한 줄로 끝
                    clientIp);


            log.info("[OrderService] 상태 변경 - {} : {} → {} (by:{} ip:{})",
                    orderNo, before, newStatus,
                    SecurityUtils.getCurrentUsername(), clientIp);

            return ApiResponse.success("상태가 변경되었습니다.");

        } catch (Exception e) {
            log.error("[OrderService] 상태 변경 실패: {}", e.getMessage());
            return ApiResponse.error("상태 변경 실패: " + e.getMessage());
        } finally {
            CountryContext.clear();
        }

    }


    @Transactional(transactionManager = "krTransactionManager")
    public ApiResponse<Void> deleteOrder(String orderNo, String clientIp) {

        try {
            CountryContext.set("KR");
            // 삭제 전 데이터 조회 (감사 로그용)
            Map<String, Object> before = countryMapper.findOrderByNo(orderNo);
            if (before == null) {
                return ApiResponse.error(404, "주문을 찾을 수 없습니다: " + orderNo);
            }

            // before_data 문자열로 변환
            String beforeData = "order_no=" + before.get("order_no")
                    + ", customer_id=" + before.get("customer_id")
                    + ", total_amount=" + before.get("total_amount")
                    + ", status=" + before.get("status");

            countryMapper.deleteOrderItems(orderNo);  // 상세 먼저
            int deleted = countryMapper.deleteOrder(orderNo);  // 헤더 삭제
            if (deleted == 0) {
                return ApiResponse.error(404, "주문을 찾을 수 없습니다: " + orderNo);
            }
            // 감사 로그
            auditLogService.logDelete(
                    "DELETE", orderNo,
                    beforeData,
                    SecurityUtils.getCurrentUsername(),clientIp);

            log.info("[OrderService] 주문 삭제 - {} (by:{} ip:{})",
                    orderNo, SecurityUtils.getCurrentUsername(), clientIp);
            return ApiResponse.success("주문이 삭제되었습니다.");

        } catch (Exception e) {
            log.error("[OrderService] 주문 삭제 실패: {}", e.getMessage());
            return ApiResponse.error("주문 삭제 실패: " + e.getMessage());
        } finally {
            CountryContext.clear();
        }
    }

    // ── private 유틸 ──────────────────────────────────
    private void checkCreditLimit(String customerId, double totalAmount) {
        if (customerId == null) return;
        try {
            Map<String, Object> customer =
                    countryMapper.findCustomerCredit(customerId);
            if (customer == null) return;

            double limit   = ((Number) customer.get("credit_limit")).doubleValue();
            double current = ((Number) customer.get("current_credit")).doubleValue();

            if (current + totalAmount > limit) {
                throw new IllegalArgumentException(
                        "신용한도 초과 - 한도: " + (long)limit
                                + " 현재잔액: " + (long)current
                                + " 요청금액: " + (long)totalAmount);
            }
        } catch (IllegalArgumentException e) {
            throw e;  // 비즈니스 예외는 다시 던짐
        } catch (Exception e) {
            log.warn("[OrderService] 신용한도 조회 실패 (무시): {}", e.getMessage());
        }
    }

    private Map<String, Object> buildPageResult(List<Map<String, Object>> list,
                                                int total, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list",       list);
        result.put("total",      total);
        result.put("page",       page);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetail(String orderNo, String country) {
        try {
            CountryContext.set(country);

            // 주문 헤더
            Map<String, Object> order = countryMapper.findOrderByNo(orderNo);
            if (order == null) throw new IllegalArgumentException(
                    "주문을 찾을 수 없습니다: " + orderNo);

            // 주문 상세 (items)
            List<Map<String, Object>> items = countryMapper.findOrderItems(orderNo);
            order.put("items", items);
            order.put("_country", country);

            return order;
        } finally {
            CountryContext.clear();
        }
    }
}