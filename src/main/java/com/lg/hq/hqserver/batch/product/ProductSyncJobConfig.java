package com.lg.hq.hqserver.batch.product;

import com.lg.hq.hqserver.batch.common.listener.DefaultJobListener;
import com.lg.hq.hqserver.batch.common.listener.DefaultStepListener;
import com.lg.hq.hqserver.batch.common.support.BatchAlertSlack;
import com.lg.hq.hqserver.batch.common.support.BatchLogSupport;
import com.lg.hq.hqserver.batch.product.ProductItemProcessor.ProductSyncResult;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 대용량 처리 전용 배치 설정.
 * 수십만~수백만 건 규모의 상품 동기화를 위한
 * 파티셔닝 + Chunk(Reader-Processor-Writer) 구조.
 *
 * 기존 BatchConfig의 Tasklet 방식과 별개로 운영.
 * 데이터 규모가 작을 땐 Tasklet, 클 땐 이 Job을 사용.
 */

@Configuration
public class ProductSyncJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final ProductCodePartitioner partitioner;
    private final ProductItemReader productItemReader;
    private final ProductItemProcessor productItemProcessor;
    private final ProductItemWriter productItemWriter;
    private final BatchLogSupport batchLog;
    private final BatchAlertSlack slack;

    @Value("${batch.partition.grid-size:4}")
    private int gridSize;

    @Value("${batch.chunk.size:1000}")
    private int chunkSize;

    // 설정에서 읽기 (좋음)
    @Value("${batch.target-countries}")
    private String targetCountries;

    public ProductSyncJobConfig(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            ProductCodePartitioner partitioner,
            ProductItemReader productItemReader,
            ProductItemProcessor productItemProcessor,
            ProductItemWriter productItemWriter,
            BatchLogSupport batchLog,
            BatchAlertSlack slack) {
        this.jobBuilderFactory    = jobBuilderFactory;
        this.stepBuilderFactory   = stepBuilderFactory;
        this.partitioner          = partitioner;
        this.productItemReader    = productItemReader;
        this.productItemProcessor = productItemProcessor;
        this.productItemWriter    = productItemWriter;
        this.batchLog             = batchLog;
        this.slack                = slack;
    }

    // ═══════════════════════════════════════════
    // JOB: 대용량 상품 동기화 (파티셔닝 + Chunk)
    // ═══════════════════════════════════════════

    @Bean
    public Job largeProductSyncJob() {
        return jobBuilderFactory.get("largeProductSyncJob")
                .incrementer(new RunIdIncrementer())
                .listener(new DefaultJobListener("largeProductSyncJob") {

                    @Override
                    public void afterJob(JobExecution je) {
                        super.afterJob(je);  // 공통 로깅

                        String status   = je.getStatus() == BatchStatus.COMPLETED ? "SUCCESS" : "FAIL";
                        String errorMsg = je.getStatus() == BatchStatus.FAILED ? extractErrorMsg(je) : null;
                        int totalWrite  = extractTotalWrite(je);

                        LocalDateTime started = je.getStartTime().toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();

                        List<String> codes = Arrays.asList(targetCountries.split(","));

                        // 국가 수로 나눠서 각각 저장
                        int perCountry = codes.size() > 0 ? totalWrite / codes.size() : totalWrite;

                        // Delta Sync 기준점 기록
                        for (String countryCode : codes) {
                            batchLog.save("LARGE_PRODUCT_SYNC", countryCode.trim(), status,
                                    started, perCountry, errorMsg);
                        }
                        if (je.getStatus() == BatchStatus.FAILED) {
                            slack.sendFail("LARGE_PRODUCT_SYNC", errorMsg, totalWrite);
                        }
                    }
                })
                .start(masterStep())
                .build();
    }

    // ═══════════════════════════════════════════
    // Master Step: 파티셔닝 + 멀티스레드 워커 실행
    // ═══════════════════════════════════════════
    @Bean
    public Step masterStep() {
        return stepBuilderFactory.get("masterStep")
                .partitioner("workerStep", partitioner)
                .gridSize(gridSize)
                .partitionHandler(productPartitionHandler())
                .build();
    }

    @Bean
    public TaskExecutorPartitionHandler productPartitionHandler() {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch-worker-");
        executor.setConcurrencyLimit(gridSize);
        handler.setTaskExecutor(executor);
        handler.setStep(workerStep());
        handler.setGridSize(gridSize);
        return handler;
    }


    // ═══════════════════════════════════════════
    // Worker Step: 파티션 범위 내에서 Chunk 처리
    // ═══════════════════════════════════════════

    @Bean
    public Step workerStep() {
        return stepBuilderFactory.get("workerStep")
                .<Map<String, Object>, ProductSyncResult>chunk(chunkSize)
                .reader(productItemReader.productReader(null, null, null, null))
                .processor(productItemProcessor)
                .writer(productItemWriter)
                .faultTolerant()
                .skipLimit(50)
                .skip(Exception.class)
                .listener(new DefaultStepListener())
                .build();
    }
}