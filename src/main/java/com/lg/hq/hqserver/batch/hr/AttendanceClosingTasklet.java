package com.lg.hq.hqserver.batch.hr;

import com.lg.hq.hqserver.batch.common.support.BatchAlertSlack;
import com.lg.hq.hqserver.batch.common.support.BatchLogSupport;
import com.lg.hq.hqserver.mapper.hq.HrMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class AttendanceClosingTasklet {

    private static final Logger log = LoggerFactory.getLogger(AttendanceClosingTasklet.class);

    private final HrMapper hrMapper;
    private final BatchLogSupport batchLog;
    private final BatchAlertSlack slack;

    public AttendanceClosingTasklet(HrMapper hrMapper,
                                    BatchLogSupport batchLog,
                                    BatchAlertSlack slack) {
        this.hrMapper = hrMapper;
        this.batchLog = batchLog;
        this.slack    = slack;
    }

    public Tasklet closingTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime started = LocalDateTime.now();
            int processed = 0;

            // 전날 날짜 (오늘 00:00에 전날 마감)
            LocalDate yesterday = LocalDate.now().minusDays(1);

            // 주말이면 스킵
            if (yesterday.getDayOfWeek() == DayOfWeek.SATURDAY
                    || yesterday.getDayOfWeek() == DayOfWeek.SUNDAY) {
                log.info("[AttendanceClosing] {} 주말 - 스킵", yesterday);
                batchLog.save("ATTENDANCE_CLOSING", null, "SUCCESS",
                        started, 0, "주말 스킵");
                return RepeatStatus.FINISHED;
            }

            String workDate = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            log.info("[AttendanceClosing] {} 근태 마감 시작", workDate);

            try {
                // 근태 미처리 직원 조회 (출근 기록 없는 직원)
                List<Map<String, Object>> absentList =
                        hrMapper.findAbsentEmployees(workDate);

                log.info("[AttendanceClosing] 미처리 직원: {}명", absentList.size());

                for (Map<String, Object> emp : absentList) {
                    String empId   = (String) emp.get("emp_id");
                    String empName = (String) emp.get("emp_name");

                    // 결근 처리
                    hrMapper.insertAttendance(
                            empId, workDate,
                            null, null,   // check_in, check_out 없음
                            0.0, 0.0,     // work_hours, overtime_hours
                            "ABSENT");    // 결근

                    log.info("[AttendanceClosing] 결근 처리 - {} ({})", empId, empName);
                    processed++;
                }

                batchLog.save("ATTENDANCE_CLOSING", null, "SUCCESS",
                        started, processed,
                        processed > 0 ? "결근처리: " + processed + "명" : null);

                if (processed > 0) {
                    slack.sendWarn("ATTENDANCE_CLOSING",
                            workDate + " 결근 " + processed + "명 자동 처리됨");
                }

                log.info("[AttendanceClosing] {} 완료 - 결근처리 {}명", workDate, processed);

            } catch (Exception e) {
                batchLog.save("ATTENDANCE_CLOSING", null, "FAIL",
                        started, processed, e.getMessage());
                slack.sendFail("ATTENDANCE_CLOSING", e.getMessage(), processed);
                throw e;
            }

            return RepeatStatus.FINISHED;
        };
    }
}