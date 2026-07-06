package com.lg.hq.hqserver.batch.quality;

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
public class DataQualityScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataQualityScheduler.class);

    private final JobLauncher jobLauncher;
    private final Job dataQualityJob;

    public DataQualityScheduler(
            JobLauncher jobLauncher,
            @Qualifier("dataQualityJob") Job dataQualityJob) {
        this.jobLauncher    = jobLauncher;
        this.dataQualityJob = dataQualityJob;
    }

    @Scheduled(cron = "0 30 8 * * *")
    public void run() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(dataQualityJob, params);
        } catch (Exception e) {
            log.error("[DataQualityScheduler] 실행 실패: {}", e.getMessage());
        }
    }
}