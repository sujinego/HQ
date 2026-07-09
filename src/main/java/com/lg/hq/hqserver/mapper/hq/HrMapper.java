package com.lg.hq.hqserver.mapper.hq;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface HrMapper {

    // ══════════════════════════════════════════════════════
    // 인사 (HR) - HQ DB 직원/근태/급여
    // ══════════════════════════════════════════════════════

    // ── 직원 ─────────────────────────────────────────────

    // 전체 직원 목록
    List<Map<String, Object>> findAllEmployees();

    // 직원 단건 조회
    Map<String, Object> findEmployeeById(@Param("empId") String empId);

    // 부서별 직원 목록
    List<Map<String, Object>> findEmployeesByDept(@Param("department") String department);

    // 직원 등록
    int insertEmployee(@Param("empId")      String empId,
                       @Param("empName")     String empName,
                       @Param("department")  String department,
                       @Param("position")    String position,
                       @Param("baseSalary")  double baseSalary,
                       @Param("hireDate")    String hireDate);

    // 직원 상태 변경 (ACTIVE/LEAVE/RESIGNED)
    int updateEmployeeStatus(@Param("empId")  String empId,
                             @Param("status") String status);

    // 직원 급여 변경
    int updateEmployeeSalary(@Param("empId")      String empId,
                             @Param("baseSalary") double baseSalary);

    // ── 근태 ─────────────────────────────────────────────

    // 특정 월 근태 목록
    List<Map<String, Object>> findAttendanceByMonth(@Param("yearMonth") String yearMonth);

    // 직원별 월 근태 집계 (급여 계산용)
    Map<String, Object> sumAttendanceByEmp(@Param("yearMonthDate") LocalDate yearMonthDate,
                                           @Param("empId")     String empId);

    // 미처리 근태 직원 목록 (근태 마감 배치용)
    List<Map<String, Object>> findAbsentEmployees(@Param("workDate") String workDate);

    // 근태 INSERT (배치에서 자동 생성)
    int insertAttendance(@Param("empId")         String empId,
                         @Param("workDate")      String workDate,
                         @Param("checkIn")       String checkIn,
                         @Param("checkOut")      String checkOut,
                         @Param("workHours")     double workHours,
                         @Param("overtimeHours") double overtimeHours,
                         @Param("status")        String status);

    // 근태 UPDATE (출퇴근 기록)
    int updateAttendance(@Param("empId")         String empId,
                         @Param("workDate")      String workDate,
                         @Param("checkOut")      String checkOut,
                         @Param("workHours")     double workHours,
                         @Param("overtimeHours") double overtimeHours);

    // ── 급여 ─────────────────────────────────────────────

    // 특정 월 급여 목록
    List<Map<String, Object>> findPayrollByMonth(@Param("yearMonth") String yearMonth);

    // 직원별 급여 이력
    List<Map<String, Object>> findPayrollByEmp(@Param("empId") String empId);

    // 급여 INSERT (배치에서 계산 후 저장)
    int upsertPayroll(@Param("empId")          String empId,
                      @Param("yearMonth")      String yearMonth,
                      @Param("baseSalary")     double baseSalary,
                      @Param("overtimePay")    double overtimePay,
                      @Param("bonus")          double bonus,
                      @Param("grossPay")       double grossPay,
                      @Param("tax")            double tax,
                      @Param("insurance")      double insurance,
                      @Param("totalDeduction") double totalDeduction,
                      @Param("netPay")         double netPay);

    // 급여 지급 상태 변경 (CALCULATED → PAID)
    int markPayrollAsPaid(@Param("yearMonth") String yearMonth);

    // 월별 급여 합계 (대시보드용)
    Map<String, Object> sumPayrollByMonth(@Param("yearMonth") String yearMonth);
}