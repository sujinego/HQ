package com.lg.hq.hqserver.controller;

import com.lg.hq.hqserver.common.ApiResponse;
import com.lg.hq.hqserver.common.SecurityUtils;
import com.lg.hq.hqserver.service.HrService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
public class HrController {
    private final HrService hrService;

    public HrController(HrService hrService) {
        this.hrService = hrService;
    }

    // ── 직원 ─────────────────────────────────────────

    // 전체 직원 목록
    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER', 'US_USER')")
    public ApiResponse<List<Map<String, Object>>> getEmployees() {
        return ApiResponse.success(hrService.getEmployees());
    }

    // 직원 단건
    @GetMapping("/employees/{empId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER', 'US_USER')")
    public ApiResponse<Map<String, Object>> getEmployee(
            @PathVariable String empId) {
        return ApiResponse.success(hrService.getEmployee(empId));
    }

    // 직원 등록
    @PostMapping("/employees")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> createEmployee(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        return hrService.createEmployee(body,
                SecurityUtils.getClientIp(request));
    }

    // 직원 상태 변경 (ACTIVE/LEAVE/RESIGNED)
    @PutMapping("/employees/{empId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateStatus(
            @PathVariable String empId,
            @RequestBody  Map<String, String> body,
            HttpServletRequest request) {
        return hrService.updateEmployeeStatus(empId,
                body.get("status"),
                SecurityUtils.getClientIp(request));
    }

    // 급여 변경
    @PutMapping("/employees/{empId}/salary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateSalary(
            @PathVariable String empId,
            @RequestBody  Map<String, Object> body,
            HttpServletRequest request) {
        double salary = ((Number) body.get("baseSalary")).doubleValue();
        return hrService.updateEmployeeSalary(empId, salary,
                SecurityUtils.getClientIp(request));
    }

    // ── 근태 ─────────────────────────────────────────

    // 월별 근태 목록
    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER', 'US_USER')")
    public ApiResponse<Map<String, Object>> getAttendance(
            @RequestParam(required = false) String yearMonth) {
        return ApiResponse.success(hrService.getAttendanceSummary(yearMonth));
    }

    // ── 급여 ─────────────────────────────────────────

    // 월별 급여 목록
    @GetMapping("/payroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER', 'US_USER')")
    public ApiResponse<Map<String, Object>> getPayroll(
            @RequestParam(required = false) String yearMonth) {
        return ApiResponse.success(hrService.getPayroll(yearMonth));
    }

    // 직원별 급여 이력
    @GetMapping("/payroll/{empId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER', 'US_USER')")
    public ApiResponse<List<Map<String, Object>>> getPayrollHistory(
            @PathVariable String empId) {
        return ApiResponse.success(hrService.getPayrollHistory(empId));
    }

    // 급여 지급 완료 처리
    @PutMapping("/payroll/{yearMonth}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> markAsPaid(
            @PathVariable String yearMonth,
            HttpServletRequest request) {
        return hrService.markAsPaid(yearMonth,
                SecurityUtils.getClientIp(request));
    }

    // ── 대시보드 요약 ──────────────────────────────────
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'KR_USER', 'US_USER')")
    public ApiResponse<Map<String, Object>> getSummary() {
        return ApiResponse.success(hrService.getHrSummary());
    }
}
