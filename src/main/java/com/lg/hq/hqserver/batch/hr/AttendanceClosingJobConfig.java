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
public class AttendanceClosingJobConfig {

    private final JobBuilderFactory  jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final AttendanceClosingTasklet tasklet;
    private final BatchAlertSlack  slack;

    public AttendanceClosingJobConfig(
            JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            AttendanceClosingTasklet tasklet,
            BatchAlertSlack slack) {
        this.jobBuilderFactory  = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.tasklet            = tasklet;
        this.slack              = slack;
    }

    @Bean
    public Job attendanceClosingJob() {
        return jobBuilderFactory.get("attendanceClosingJob")
                .incrementer(new RunIdIncrementer())
                .listener(new DefaultJobListener("attendanceClosingJob") {
                    @Override
                    public void afterJob(JobExecution je) {
                        super.afterJob(je);
                        if (je.getStatus() == BatchStatus.FAILED) {
                            slack.sendFail("ATTENDANCE_CLOSING",
                                    extractErrorMsg(je), 0);
                        }
                    }
                })
                .start(attendanceClosingStep())
                .build();
    }

    @Bean
    public Step attendanceClosingStep() {
        return stepBuilderFactory.get("attendanceClosingStep")
                .tasklet(tasklet.closingTasklet())
                .build();
    }
}