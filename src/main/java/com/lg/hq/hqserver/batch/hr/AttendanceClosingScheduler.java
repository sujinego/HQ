package com.lg.hq.hqserver.batch.hr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttendanceClosingScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(AttendanceClosingScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job         attendanceClosingJob;

    public AttendanceClosingScheduler(
            JobLauncher jobLauncher,
            @Qualifier("attendanceClosingJob") Job attendanceClosingJob) {
        this.jobLauncher          = jobLauncher;
        this.attendanceClosingJob = attendanceClosingJob;
    }

    // 매일 00:00 전날 근태 마감
    @Scheduled(cron = "0 0 0 * * *")
    public void run() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(attendanceClosingJob, params);
        } catch (Exception e) {
            log.error("[AttendanceClosingScheduler] 실행 실패: {}", e.getMessage());
        }
    }
}