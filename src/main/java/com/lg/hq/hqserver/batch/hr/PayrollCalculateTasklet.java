package com.lg.hq.hqserver.batch.hr;

import com.lg.hq.hqserver.batch.common.support.BatchAlertSlack;
import com.lg.hq.hqserver.batch.common.support.BatchLogSupport;
import com.lg.hq.hqserver.mapper.hq.HrMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class PayrollCalculateTasklet {

    private static final Logger log =
            LoggerFactory.getLogger(PayrollCalculateTasklet.class);

    // 급여 계산 상수
    private static final double TAX_RATE       = 0.033;  // 소득세 3.3%
    private static final double INSURANCE_RATE = 0.0932; // 4대보험 9.32%
    private static final double OVERTIME_RATE  = 1.5;    // 초과근무 1.5배
    private static final int    STANDARD_HOURS = 8;      // 일 표준 근무시간

    private final HrMapper hrMapper;
    private final BatchLogSupport batchLog;
    private final BatchAlertSlack slack;

    public PayrollCalculateTasklet(HrMapper hrMapper,
                                   BatchLogSupport batchLog,
                                   BatchAlertSlack slack) {
        this.hrMapper = hrMapper;
        this.batchLog = batchLog;
        this.slack    = slack;
    }

    public Tasklet calculateTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime started    = LocalDateTime.now();
            String yearMonth = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM"));
            LocalDate yearMonthDate = LocalDate.now().withDayOfMonth(1);
            int processed = 0;

            log.info("[PayrollCalculate] {} 급여 계산 시작", yearMonth);

            try {
                // 1. 활성 직원 전체 조회
                List<Map<String, Object>> employees = hrMapper.findAllEmployees();
                log.info("[PayrollCalculate] 대상 직원: {}명", employees.size());

                // 2. 이번 달 영업일 수 계산
                int businessDays = getBusinessDays(yearMonth);
                log.info("[PayrollCalculate] {} 영업일수: {}일", yearMonth, businessDays);

                for (Map<String, Object> emp : employees) {
                    String empId      = (String) emp.get("emp_id");
                    double baseSalary = ((Number) emp.get("base_salary")).doubleValue();

                    // 3. 근태 집계 조회
                    Map<String, Object> attendance =
                            hrMapper.sumAttendanceByEmp(yearMonthDate, empId);

                    double workDays    = attendance != null && attendance.get("work_days") != null
                            ? ((Number) attendance.get("work_days")).doubleValue() : 0;
                    double totalOvertime = attendance != null && attendance.get("total_overtime") != null
                            ? ((Number) attendance.get("total_overtime")).doubleValue() : 0;
                    double absentDays  = attendance != null && attendance.get("absent_days") != null
                            ? ((Number) attendance.get("absent_days")).doubleValue() : 0;

                    // 4. 급여 계산
                    // 일할 계산 (결근일 차감)
                    double actualDays   = Math.max(0, workDays - absentDays);
                    double dailySalary  = baseSalary / businessDays;
                    double earnedBase   = dailySalary * actualDays;

                    // 시급 계산 (월급 / 영업일 / 8시간)
                    double hourlyRate   = dailySalary / STANDARD_HOURS;

                    // 초과근무수당
                    double overtimePay  = hourlyRate * totalOvertime * OVERTIME_RATE;

                    // 총지급액
                    double grossPay     = earnedBase + overtimePay;

                    // 공제 계산
                    double tax          = grossPay * TAX_RATE;
                    double insurance    = grossPay * INSURANCE_RATE;
                    double totalDeduct  = tax + insurance;

                    // 실지급액
                    double netPay       = grossPay - totalDeduct;

                    // 5. DB 저장
                    hrMapper.upsertPayroll(
                            empId, yearMonth,
                            (double) Math.round(earnedBase),    // 기본급
                            (double) Math.round(overtimePay),   // 초과수당
                            0.0,                                // 성과급
                            (double) Math.round(grossPay),      // 총지급액
                            (double) Math.round(tax),           // 소득세
                            (double) Math.round(insurance),     // 4대보험
                            (double) Math.round(totalDeduct),   // 총공제액
                            (double) Math.round(netPay));       // 실지급액

                    log.info("[PayrollCalculate] {} - 기본급:{} 초과수당:{} 실지급:{}",
                            empId,
                            (double) Math.round(earnedBase),
                            (double) Math.round(overtimePay),
                            (double) Math.round(netPay));

                    processed++;
                }

                // 6. 완료 알림
                Map<String, Object> summary = hrMapper.sumPayrollByMonth(yearMonth);
                double totalNet = summary != null && summary.get("total_net") != null
                        ? ((Number) summary.get("total_net")).doubleValue() : 0;

                batchLog.save("PAYROLL_CALCULATE", null, "SUCCESS",
                        started, processed, null);

                slack.sendWarn("PAYROLL_CALCULATE",
                        yearMonth + " 급여 계산 완료 - " + processed + "명 / "
                                + "총 실지급액: ₩" + String.format("%,.0f", totalNet));

                log.info("[PayrollCalculate] {} 완료 - {}명 처리", yearMonth, processed);

            } catch (Exception e) {
                batchLog.save("PAYROLL_CALCULATE", null, "FAIL",
                        started, processed, e.getMessage());
                slack.sendFail("PAYROLL_CALCULATE", e.getMessage(), processed);
                throw e;
            }

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * 특정 월의 영업일 수 계산 (주말 제외)
     */
    private int getBusinessDays(String yearMonth) {
        YearMonth ym    = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end   = ym.atEndOfMonth();
        int count = 0;
        LocalDate date  = start;
        while (!date.isAfter(end)) {
            if (date.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                    && date.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }
}