package com.lg.hq.hqserver.batch.exchange;

import com.lg.hq.hqserver.batch.common.listener.DefaultJobListener;
import com.lg.hq.hqserver.batch.common.support.BatchAlertSlack;
import com.lg.hq.hqserver.batch.common.support.BatchLogSupport;
import com.lg.hq.hqserver.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

@Configuration
public class ExchangeRateJobConfig {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateJobConfig.class);

    private final JobBuilderFactory   jobBuilderFactory;
    private final StepBuilderFactory  stepBuilderFactory;
    private final CacheService cacheService;
    private final JdbcTemplate        hqJdbc;
    private final ExchangeRateApiClient apiClient;
    private final BatchLogSupport     batchLog;
    private final BatchAlertSlack slack;

    public ExchangeRateJobConfig(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            CacheService cacheService,
            @Qualifier("hqJdbc") JdbcTemplate hqJdbc,
            ExchangeRateApiClient apiClient,
            BatchLogSupport batchLog,
            BatchAlertSlack slack) {
        this.jobBuilderFactory  = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.cacheService = cacheService;
        this.hqJdbc             = hqJdbc;
        this.apiClient          = apiClient;
        this.batchLog           = batchLog;
        this.slack              = slack;
    }

    @Bean
    public Job exchangeRateJob() {
        return jobBuilderFactory.get("exchangeRateJob")
                .incrementer(new RunIdIncrementer())
                .listener(new DefaultJobListener("exchangeRateJob") {
                    @Override
                    public void afterJob(JobExecution je) {
                        super.afterJob(je);
                        if (je.getStatus() == BatchStatus.COMPLETED) {
                            cacheService.evictExchangeRates();  // 캐시 즉시 만료
                            log.info("[ExchangeRateJob] 환율 캐시 초기화 완료");
                        }
                        if (je.getStatus() == BatchStatus.FAILED) {
                            slack.sendFail("EXCHANGE_RATE", extractErrorMsg(je), 0);
                        }
                    }
                })
                .start(exchangeRateStep())
                .build();
    }

    @Bean
    public Step exchangeRateStep() {
        return stepBuilderFactory.get("exchangeRateStep")
                .tasklet((contribution, chunkContext) -> {
                    LocalDateTime started = LocalDateTime.now();
                    log.info("[EXCHANGE_RATE] 환율 업데이트 시작");
                    int processed = 0;
                    int maxRetry  = 3;
                    int retry     = 0;
                    Exception lastEx = null;

                    while (retry < maxRetry) {
                        try {
                            Object[][] rates = apiClient.fetchTodayRates();
                            for (Object[] rate : rates) {
                                hqJdbc.update(
                                        "INSERT INTO exchange_rate " +
                                                "(rate_date, currency_code, rate_to_krw, created_at) " +
                                                "VALUES (CURDATE(), ?, ?, NOW()) " +
                                                "ON DUPLICATE KEY UPDATE " +
                                                "rate_to_krw=VALUES(rate_to_krw), created_at=NOW()",
                                        rate[0], rate[1]);
                                processed++;
                            }
                            batchLog.save("EXCHANGE_RATE", null, "SUCCESS",
                                    started, processed, null);
                            log.info("[ExchangeRateJob] 완료 - {}건 ({}회 시도)",
                                    processed, retry + 1);
                            return RepeatStatus.FINISHED;

                        } catch (Exception e) {
                            lastEx = e;
                            retry++;
                            log.warn("[ExchangeRateJob] {}회 실패, {}",
                                    retry, retry < maxRetry ? "재시도..." : "최종 실패");
                            if (retry < maxRetry) {
                                try { Thread.sleep(2000L * retry); }
                                catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }

                    // 3회 모두 실패
                    batchLog.save("EXCHANGE_RATE", null, "FAIL",
                            started, 0, lastEx.getMessage());
                    slack.sendFail("EXCHANGE_RATE", lastEx.getMessage(), 0);
                    throw new RuntimeException(
                            "환율 업데이트 " + maxRetry + "회 재시도 후 실패", lastEx);
                })
                .build();
    }
}