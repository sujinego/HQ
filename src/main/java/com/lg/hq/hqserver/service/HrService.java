package com.lg.hq.hqserver.service;

import com.lg.hq.hqserver.common.ApiResponse;
import com.lg.hq.hqserver.common.SecurityUtils;
import com.lg.hq.hqserver.mapper.hq.HrMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class HrService {
    private static final Logger log = LoggerFactory.getLogger(HrService.class);

    private final HrMapper hrMapper;
    private final AuditLogService auditLogService;

    public HrService(HrMapper hrMapper, AuditLogService auditLogService) {
        this.hrMapper        = hrMapper;
        this.auditLogService = auditLogService;
    }

    // ══════════════════════════════════════════════════
    // 직원
    // ══════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEmployees() {
        return hrMapper.findAllEmployees();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEmployee(String empId) {
        Map<String, Object> emp = hrMapper.findEmployeeById(empId);
        if (emp == null) throw new IllegalArgumentException(
                "직원을 찾을 수 없습니다: " + empId);
        return emp;
    }

    @Transactional(transactionManager = "hqTransactionManager")
    public ApiResponse<Void> createEmployee(Map<String, Object> req,
                                            String clientIp) {
        String empId      = (String) req.get("empId");
        String empName    = (String) req.get("empName");
        String department = (String) req.get("department");
        String position   = (String) req.get("position");
        double baseSalary = req.get("baseSalary") != null
                ? ((Number) req.get("baseSalary")).doubleValue() : 0;
        String hireDate   = (String) req.get("hireDate");

        if (empId == null || empName == null) {
            return ApiResponse.error(400, "직원ID와 이름은 필수입니다.");
        }

        hrMapper.insertEmployee(
                empId, empName, department, position, baseSalary, hireDate);

        auditLogService.logCreate("EMPLOYEE", empId,
                SecurityUtils.getCurrentUsername(), clientIp);

        log.info("[HrService] 직원 등록 - {} {} (by:{})",
                empId, empName, SecurityUtils.getCurrentUsername());

        return ApiResponse.success("직원이 등록되었습니다.");
    }

    @Transactional(transactionManager = "hqTransactionManager")
    public ApiResponse<Void> updateEmployeeStatus(String empId,
                                                  String status,
                                                  String clientIp) {
        String before = (String) hrMapper.findEmployeeById(empId).get("status");
        hrMapper.updateEmployeeStatus(empId, status);

        auditLogService.logUpdate("EMPLOYEE", empId,
                "status=" + before, "status=" + status,
                SecurityUtils.getCurrentUsername(), clientIp);

        return ApiResponse.success("상태가 변경되었습니다.");
    }

    @Transactional(transactionManager = "hqTransactionManager")
    public ApiResponse<Void> updateEmployeeSalary(String empId,
                                                  double baseSalary,
                                                  String clientIp) {
        Map<String, Object> emp = hrMapper.findEmployeeById(empId);
        String before = String.valueOf(emp.get("base_salary"));

        hrMapper.updateEmployeeSalary(empId, baseSalary);

        auditLogService.logUpdate("EMPLOYEE", empId,
                "base_salary=" + before,
                "base_salary=" + (long) baseSalary,
                SecurityUtils.getCurrentUsername(), clientIp);

        log.info("[HrService] 급여 변경 - {} : {} → {} (by:{})",
                empId, before, (long) baseSalary,
                SecurityUtils.getCurrentUsername());

        return ApiResponse.success("급여가 변경되었습니다.");
    }

    // ══════════════════════════════════════════════════
    // 근태
    // ══════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAttendance(String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return hrMapper.findAttendanceByMonth(yearMonth);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAttendanceSummary(String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        List<Map<String, Object>> list = hrMapper.findAttendanceByMonth(yearMonth);

        // 부서별 집계
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("yearMonth", yearMonth);
        result.put("totalRecords", list.size());

        long absentCount = list.stream()
                .filter(a -> "ABSENT".equals(a.get("status"))).count();
        long lateCount = list.stream()
                .filter(a -> "LATE".equals(a.get("status"))).count();

        result.put("absentCount", absentCount);
        result.put("lateCount",   lateCount);
        result.put("list",        list);
        return result;
    }

    // ══════════════════════════════════════════════════
    // 급여
    // ══════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getPayroll(String yearMonth) {
        if (yearMonth == null || yearMonth.isEmpty()) {
            yearMonth = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        List<Map<String, Object>> list  = hrMapper.findPayrollByMonth(yearMonth);
        Map<String, Object>       total = hrMapper.sumPayrollByMonth(yearMonth);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("yearMonth", yearMonth);
        result.put("list",      list);
        result.put("summary",   total);
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPayrollHistory(String empId) {
        return hrMapper.findPayrollByEmp(empId);
    }

    @Transactional(transactionManager = "hqTransactionManager")
    public ApiResponse<Void> markAsPaid(String yearMonth, String clientIp) {
        int updated = hrMapper.markPayrollAsPaid(yearMonth);

        auditLogService.logUpdate("PAYROLL", yearMonth,
                "status=CALCULATED", "status=PAID",
                SecurityUtils.getCurrentUsername(), clientIp);

        log.info("[HrService] 급여 지급 완료 - {} {}명 (by:{})",
                yearMonth, updated, SecurityUtils.getCurrentUsername());

        return ApiResponse.success(yearMonth + " 급여 지급 완료 처리 (" + updated + "명)");
    }

    // ── 대시보드용 HR 요약 ────────────────────────────
    @Transactional(readOnly = true)
    public Map<String, Object> getHrSummary() {
        String thisMonth = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String today     = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        Map<String, Object> result = new LinkedHashMap<>();

        // 직원 현황
        List<Map<String, Object>> employees = hrMapper.findAllEmployees();
        result.put("totalEmployees", employees.size());

        // 오늘 결근자
        List<Map<String, Object>> absentToday = hrMapper.findAbsentEmployees(today);
        result.put("absentToday", absentToday.size());

        // 이번 달 급여 합계
        Map<String, Object> payrollSummary = hrMapper.sumPayrollByMonth(thisMonth);
        result.put("monthlyPayroll", payrollSummary);

        return result;
    }
}
