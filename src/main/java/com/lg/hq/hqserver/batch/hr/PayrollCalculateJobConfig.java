package com.lg.hq.hqserver.batch.hr;

import com.lg.hq.hqserver.batch.common.listener.DefaultJobListener;
import com.lg.hq.hqserver.batch.common.support.BatchAlertSlack;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayrollCalculateJobConfig {

    private final JobBuilderFactory    jobBuilderFactory;
    private final StepBuilderFactory   stepBuilderFactory;
    private final PayrollCalculateTasklet tasklet;
    private final BatchAlertSlack    slack;

    public PayrollCalculateJobConfig(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            PayrollCalculateTasklet tasklet,
            BatchAlertSlack slack) {
        this.jobBuilderFactory  = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.tasklet            = tasklet;
        this.slack              = slack;
    }

    @Bean
    public Job payrollCalculateJob() {
        return jobBuilderFactory.get("payrollCalculateJob")
                .incrementer(new RunIdIncrementer())
                .listener(new DefaultJobListener("payrollCalculateJob") {
                    @Override
                    public void afterJob(JobExecution je) {
                        super.afterJob(je);
                        if (je.getStatus() == BatchStatus.FAILED) {
                            slack.sendFail("PAYROLL_CALCULATE",
                                    extractErrorMsg(je), 0);
                        }
                    }
                })
                .start(payrollCalculateStep())
                .build();
    }

    @Bean
    public Step payrollCalculateStep() {
        return stepBuilderFactory.get("payrollCalculateStep")
                .tasklet(tasklet.calculateTasklet())
                .build();
    }
}