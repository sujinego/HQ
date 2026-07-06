package com.lg.hq.hqserver.batch.sales;

import com.lg.hq.hqserver.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lg.hq.hqserver.batch.common.listener.DefaultJobListener;
import com.lg.hq.hqserver.batch.common.support.BatchAlertSlack;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SalesAggregateJobConfig {

    private static final Logger log = LoggerFactory.getLogger(SalesAggregateJobConfig.class);

    private final JobBuilderFactory  jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final CacheService cacheService;
    private final SalesAggregateTasklet tasklet;
    private final BatchAlertSlack slack;

    public SalesAggregateJobConfig(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            CacheService cacheService,
            SalesAggregateTasklet tasklet,
            BatchAlertSlack slack) {
        this.jobBuilderFactory  = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.cacheService = cacheService;
        this.tasklet            = tasklet;
        this.slack              = slack;
    }

    @Bean
    public Job salesAggregateJob() {
        return jobBuilderFactory.get("salesAggregateJob")
                .incrementer(new RunIdIncrementer())
                .listener(new DefaultJobListener("salesAggregateJob") {
                    @Override
                    public void afterJob(JobExecution je) {
                        super.afterJob(je);
                        if (je.getStatus() == BatchStatus.COMPLETED) {
                            cacheService.evictMonthlySales();  // 집계 완료 후 캐시 즉시 만료
                            log.info("[SalesAggregateJob] 월 매출 캐시 초기화 완료");
                        }
                        if (je.getStatus() == BatchStatus.FAILED) {
                            slack.sendFail("SALES_AGGREGATE", extractErrorMsg(je), 0);
                        }
                    }
                })
                .start(salesAggregateKrStep())
                .next(salesAggregateUsStep())
                .build();
    }

    @Bean
    public Step salesAggregateKrStep() {
        return stepBuilderFactory.get("salesAggregateKrStep")
                .tasklet(tasklet.krTasklet())
                .build();
    }

    @Bean
    public Step salesAggregateUsStep() {
        return stepBuilderFactory.get("salesAggregateUsStep")
                .tasklet(tasklet.usTasklet())
                .build();
    }
}