package com.lg.hq.hqserver.batch.exchange;

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
public class ExchangeRateScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job exchangeRateJob;

    public ExchangeRateScheduler(
            JobLauncher jobLauncher,
            @Qualifier("exchangeRateJob") Job exchangeRateJob) {
        this.jobLauncher     = jobLauncher;
        this.exchangeRateJob = exchangeRateJob;
    }

    @Scheduled(cron = "0 0 14 * * *")
    public void run() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(exchangeRateJob, params);
        } catch (Exception e) {
            log.error("[ExchangeRateScheduler] 실행 실패: {}", e.getMessage());
        }
    }
}