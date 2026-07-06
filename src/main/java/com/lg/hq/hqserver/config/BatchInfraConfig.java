package com.lg.hq.hqserver.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableBatchProcessing
@EnableRetry
public class BatchInfraConfig {
    // @EnableBatchProcessing 이 하나의 어노테이션이
    // JobBuilderFactory, StepBuilderFactory, JobLauncher,
    // JobRepository, JobExplorer 등 배치 인프라 빈을 자동 등록
    // @EnableRetry:
    //   @Retryable 어노테이션 활성화
    //   환율 배치의 재시도 로직에 필요
}
