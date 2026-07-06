package com.lg.hq.hqserver.controller;

import com.lg.hq.hqserver.common.ApiResponse;
import com.lg.hq.hqserver.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpi")
    public ApiResponse<Map<String, Object>> getKpi() {
        return ApiResponse.success(dashboardService.getKpi());
    }

    @GetMapping("/recent-orders")
    public ApiResponse<Map<String, Object>> getRecentOrders() {
        return ApiResponse.success(dashboardService.getRecentOrders());
    }

    @GetMapping("/sales-by-country")
    public ApiResponse<List<Map<String, Object>>> getSalesByCountry() {
        return ApiResponse.success(dashboardService.getSalesByCountry());
    }
}