package com.lg.hq.hqserver.mapper.hq;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface HrMapper {

    // ══════════════════════════════════════════════════════
// 인사 (HR) - HQ DB 직원/근태/급여
// ══════════════════════════════════════════════════════

// ── 직원 ─────────────────────────────────────────────

    // 전체 직원 목록
    @Select("SELECT emp_id, emp_name, department, position, " +
            "base_salary, hire_date, status, created_at " +
            "FROM employee WHERE status = 'ACTIVE' " +
            "ORDER BY department, emp_id")
    List<Map<String, Object>> findAllEmployees();

    // 직원 단건 조회
    @Select("SELECT * FROM employee WHERE emp_id = #{empId}")
    Map<String, Object> findEmployeeById(@Param("empId") String empId);

    // 부서별 직원 목록
    @Select("SELECT emp_id, emp_name, department, position, base_salary, hire_date " +
            "FROM employee WHERE status = 'ACTIVE' AND department = #{department} " +
            "ORDER BY emp_id")
    List<Map<String, Object>> findEmployeesByDept(@Param("department") String department);

    // 직원 등록
    @Insert("INSERT INTO employee " +
            "(emp_id, emp_name, department, position, base_salary, hire_date, status) " +
            "VALUES (#{empId}, #{empName}, #{department}, #{position}, " +
            "#{baseSalary}, #{hireDate}, 'ACTIVE')")
    int insertEmployee(@Param("empId")       String empId,
                       @Param("empName")     String empName,
                       @Param("department")  String department,
                       @Param("position")    String position,
                       @Param("baseSalary")  double baseSalary,
                       @Param("hireDate")    String hireDate);

    // 직원 상태 변경 (ACTIVE/LEAVE/RESIGNED)
    @Update("UPDATE employee SET status = #{status}, updated_at = NOW() " +
            "WHERE emp_id = #{empId}")
    int updateEmployeeStatus(@Param("empId")  String empId,
                             @Param("status") String status);

    // 직원 급여 변경
    @Update("UPDATE employee SET base_salary = #{baseSalary}, updated_at = NOW() " +
            "WHERE emp_id = #{empId}")
    int updateEmployeeSalary(@Param("empId")      String empId,
                             @Param("baseSalary") double baseSalary);

// ── 근태 ─────────────────────────────────────────────

    // 특정 월 근태 목록
    @Select("SELECT a.*, e.emp_name, e.department " +
            "FROM attendance a " +
            "JOIN employee e ON a.emp_id = e.emp_id " +
            "WHERE FORMATDATETIME(a.work_date, 'yyyy-MM') = #{yearMonth} " +
            "ORDER BY a.emp_id, a.work_date")
    List<Map<String, Object>> findAttendanceByMonth(
            @Param("yearMonth") String yearMonth);

    // 직원별 월 근태 집계 (급여 계산용)
    @Select("SELECT emp_id, " +
            "COUNT(*) AS work_days, " +
            "SUM(work_hours) AS total_work_hours, " +
            "SUM(overtime_hours) AS total_overtime, " +
            "SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_days, " +
            "SUM(CASE WHEN status = 'LATE'   THEN 1 ELSE 0 END) AS late_days " +
            "FROM attendance " +
            "WHERE FORMATDATETIME(work_date, 'yyyy-MM') = #{yearMonth} " +
            "AND emp_id = #{empId}")
    Map<String, Object> sumAttendanceByEmp(@Param("yearMonth") String yearMonth,
                                           @Param("empId")     String empId);

    // 미처리 근태 직원 목록 (근태 마감 배치용)
    @Select("SELECT e.emp_id, e.emp_name " +
            "FROM employee e " +
            "WHERE e.status = 'ACTIVE' " +
            "AND e.emp_id NOT IN ( " +
            "  SELECT emp_id FROM attendance WHERE work_date = #{workDate} " +
            ")")
    List<Map<String, Object>> findAbsentEmployees(@Param("workDate") String workDate);

    // 근태 INSERT (배치에서 자동 생성)
    @Insert("INSERT INTO attendance " +
            "(emp_id, work_date, check_in, check_out, work_hours, overtime_hours, status) " +
            "VALUES (#{empId}, #{workDate}, #{checkIn}, #{checkOut}, " +
            "#{workHours}, #{overtimeHours}, #{status})")
    int insertAttendance(@Param("empId")         String empId,
                         @Param("workDate")      String workDate,
                         @Param("checkIn")       String checkIn,
                         @Param("checkOut")      String checkOut,
                         @Param("workHours")     double workHours,
                         @Param("overtimeHours") double overtimeHours,
                         @Param("status")        String status);

    // 근태 UPDATE (출퇴근 기록)
    @Update("UPDATE attendance SET check_out = #{checkOut}, " +
            "work_hours = #{workHours}, overtime_hours = #{overtimeHours} " +
            "WHERE emp_id = #{empId} AND work_date = #{workDate}")
    int updateAttendance(@Param("empId")         String empId,
                         @Param("workDate")      String workDate,
                         @Param("checkOut")      String checkOut,
                         @Param("workHours")     double workHours,
                         @Param("overtimeHours") double overtimeHours);

// ── 급여 ─────────────────────────────────────────────

    // 특정 월 급여 목록
    @Select("SELECT p.*, e.emp_name, e.department, e.position " +
            "FROM payroll p " +
            "JOIN employee e ON p.emp_id = e.emp_id " +
            "WHERE p.pay_year_month = #{yearMonth} " +
            "ORDER BY e.department, p.emp_id")
    List<Map<String, Object>> findPayrollByMonth(@Param("yearMonth") String yearMonth);

    // 직원별 급여 이력
    @Select("SELECT p.*, e.emp_name " +
            "FROM payroll p " +
            "JOIN employee e ON p.emp_id = e.emp_id " +
            "WHERE p.emp_id = #{empId} " +
            "ORDER BY p.pay_year_month DESC " +
            "LIMIT 12")
    List<Map<String, Object>> findPayrollByEmp(@Param("empId") String empId);

    // 급여 INSERT (배치에서 계산 후 저장)
    @Insert("INSERT INTO payroll " +
            "(emp_id, pay_year_month, base_salary, overtime_pay, bonus, " +
            "gross_pay, tax, insurance, total_deduction, net_pay, status) " +
            "VALUES (#{empId}, #{yearMonth}, #{baseSalary}, #{overtimePay}, #{bonus}, " +
            "#{grossPay}, #{tax}, #{insurance}, #{totalDeduction}, #{netPay}, 'CALCULATED') " +
            "ON DUPLICATE KEY UPDATE " +
            "base_salary=VALUES(base_salary), overtime_pay=VALUES(overtime_pay), " +
            "bonus=VALUES(bonus), gross_pay=VALUES(gross_pay), " +
            "tax=VALUES(tax), insurance=VALUES(insurance), " +
            "total_deduction=VALUES(total_deduction), net_pay=VALUES(net_pay)")
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
    @Update("UPDATE payroll SET status = 'PAID' " +
            "WHERE pay_year_month = #{yearMonth}")
    int markPayrollAsPaid(@Param("yearMonth") String yearMonth);

    // 월별 급여 합계 (대시보드용)
    @Select("SELECT " +
            "COUNT(*) AS total_employees, " +
            "SUM(gross_pay) AS total_gross, " +
            "SUM(total_deduction) AS total_deduction, " +
            "SUM(net_pay) AS total_net " +
            "FROM payroll " +
            "WHERE pay_year_month = #{yearMonth}")
    Map<String, Object> sumPayrollByMonth(@Param("yearMonth") String yearMonth);
}
