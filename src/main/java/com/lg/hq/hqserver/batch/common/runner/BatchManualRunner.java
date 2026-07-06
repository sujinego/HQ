package com.lg.hq.hqserver.batch.common.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 배치 수동 실행 공통 컴포넌트.
 * BatchMonitorController에서 /api/batch/run/{jobKey} 호출 시 사용.
 */
@Component
public class BatchManualRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchManualRunner.class);

    private final JobLauncher jobLauncher;
    private final Job largeProductSyncJob;
    private final Job exchangeRateJob;
    private final Job salesAggregateJob;
    private final Job dataQualityJob;

    private final Job attendanceClosingJob;
    private final Job payrollCalculateJob;

    public BatchManualRunner(
            JobLauncher jobLauncher,
            @Qualifier("largeProductSyncJob") Job largeProductSyncJob,
            @Qualifier("exchangeRateJob")     Job exchangeRateJob,
            @Qualifier("salesAggregateJob")   Job salesAggregateJob,
            @Qualifier("dataQualityJob")      Job dataQualityJob,
            @Qualifier("attendanceClosingJob")  Job attendanceClosingJob,
            @Qualifier("payrollCalculateJob")   Job payrollCalculateJob) {
        this.jobLauncher = jobLauncher;
        this.largeProductSyncJob = largeProductSyncJob;
        this.exchangeRateJob     = exchangeRateJob;
        this.salesAggregateJob   = salesAggregateJob;
        this.dataQualityJob      = dataQualityJob;
        this.attendanceClosingJob = attendanceClosingJob;
        this.payrollCalculateJob = payrollCalculateJob;
    }

    public String run(String jobKey) {
        try {
            Job job = resolve(jobKey);
            if (job == null) return "알 수 없는 배치: " + jobKey;

            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(job, params);
            return "[" + jobKey + "] 실행 완료";

        } catch (Exception e) {
            log.error("[BatchManualRunner] {} 실행 실패: {}", jobKey, e.getMessage());
            return "[" + jobKey + "] 실행 실패: " + e.getMessage();
        }
    }

    private Job resolve(String jobKey) {
        switch (jobKey) {
            case "product-sync":    return largeProductSyncJob;
            case "exchange-rate":   return exchangeRateJob;
            case "sales-aggregate": return salesAggregateJob;
            case "data-quality":    return dataQualityJob;
            case "attendance-closing":   return attendanceClosingJob;
            case "payroll-calculate":    return payrollCalculateJob;
            default:                return null;
        }
    }
}