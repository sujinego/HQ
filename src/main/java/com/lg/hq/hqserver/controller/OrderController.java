package com.lg.hq.hqserver.controller;

import com.lg.hq.hqserver.common.ApiResponse;
import com.lg.hq.hqserver.common.SecurityUtils;
import com.lg.hq.hqserver.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ── 조회: 전국가 전체 권한 ────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER', 'US_USER')")
    public ApiResponse<Map<String, Object>> getOrders(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Map<String, Object> result = orderService.getOrders(
                country, keyword, status, startDate, endDate, page, size);
        return ApiResponse.success(result);
    }

    // 주문 상세 조회
    @GetMapping("/{orderNo}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER', 'US_USER')")
    public ApiResponse<Map<String, Object>> getOrderDetail(
            @PathVariable String orderNo,
            @RequestParam(required = false, defaultValue = "KR") String country) {
        return ApiResponse.success(orderService.getOrderDetail(orderNo, country));
    }


    //한국 지사 주문 등록 (KR 권한 담당자만)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER')")
    public ApiResponse<Void> createOrder(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String clientIp = SecurityUtils.getClientIp(request);
        return orderService.createOrder(body, clientIp);
    }



    //한국 지사 상태 변경 (KR 권한 담당자만)
    @PutMapping("/{orderNo}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER')")
    public ApiResponse<Void> updateStatus(
            @PathVariable String orderNo,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String clientIp = SecurityUtils.getClientIp(request);
        return orderService.updateOrderStatus(
                orderNo, body.get("status"), clientIp);
    }


    // 삭제: ADMIN만
    @DeleteMapping("/{orderNo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteOrder(
            @PathVariable String orderNo,
            HttpServletRequest request) {

        String clientIp = SecurityUtils.getClientIp(request);
        return orderService.deleteOrder(orderNo, clientIp);
    }


}