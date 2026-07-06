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
public class PayrollCalculateScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(PayrollCalculateScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job         payrollCalculateJob;

    public PayrollCalculateScheduler(
            JobLauncher jobLauncher,
            @Qualifier("payrollCalculateJob") Job payrollCalculateJob) {
        this.jobLauncher        = jobLauncher;
        this.payrollCalculateJob = payrollCalculateJob;
    }

    // 매월 25일 09:00 급여 계산
    @Scheduled(cron = "0 0 9 25 * *")
    public void run() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(payrollCalculateJob, params);
        } catch (Exception e) {
            log.error("[PayrollCalculateScheduler] 실행 실패: {}", e.getMessage());
        }
    }
}