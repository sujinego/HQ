package com.lg.hq.hqserver.batch.quality;

import com.lg.hq.hqserver.batch.common.listener.DefaultJobListener;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ═══════════════════════════════════════════
// JOB 4: 데이터 품질 검증
// ═══════════════════════════════════════════

@Configuration
public class DataQualityJobConfig {

    private final JobBuilderFactory  jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataQualityTasklet tasklet;

    public DataQualityJobConfig(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            DataQualityTasklet tasklet) {
        this.jobBuilderFactory  = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.tasklet            = tasklet;
    }

    @Bean
    public Job dataQualityJob() {
        return jobBuilderFactory.get("dataQualityJob")
                .incrementer(new RunIdIncrementer())
                .listener(new DefaultJobListener("dataQualityJob"))
                .start(dataQualityStep())
                .build();
    }

    @Bean
    public Step dataQualityStep() {
        return stepBuilderFactory.get("dataQualityStep")
                .tasklet(tasklet.qualityTasklet())
                .build();
    }
}