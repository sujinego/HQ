package com.lg.hq.hqserver.batch.sales;

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
public class SalesAggregateScheduler {

    private static final Logger log = LoggerFactory.getLogger(SalesAggregateScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job salesAggregateJob;

    public SalesAggregateScheduler(
            JobLauncher jobLauncher,
            @Qualifier("salesAggregateJob") Job salesAggregateJob) {
        this.jobLauncher      = jobLauncher;
        this.salesAggregateJob = salesAggregateJob;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void run() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(salesAggregateJob, params);
        } catch (Exception e) {
            log.error("[SalesAggregateScheduler] 실행 실패: {}", e.getMessage());
        }
    }
}