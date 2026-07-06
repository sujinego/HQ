package com.lg.hq.hqserver.batch.product;

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
public class ProductSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProductSyncScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job largeProductSyncJob;

    public ProductSyncScheduler(
            JobLauncher jobLauncher,
            @Qualifier("largeProductSyncJob") Job largeProductSyncJob) {
        this.jobLauncher        = jobLauncher;
        this.largeProductSyncJob = largeProductSyncJob;
    }

    @Scheduled(fixedDelay = 600000)
    public void run() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(largeProductSyncJob, params);
        } catch (Exception e) {
            log.error("[ProductSyncScheduler] 실행 실패: {}", e.getMessage());
        }
    }
}