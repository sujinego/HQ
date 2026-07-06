package com.lg.hq.hqserver.batch.common.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

import java.time.format.DateTimeFormatter;

/**
 * 모든 Job에 공통 적용되는 리스너.
 * Job별 추가 동작이 필요하면 이 클래스를 상속받아 override.
 */
public class DefaultJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(DefaultJobListener.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String jobName;

    public DefaultJobListener(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public void beforeJob(JobExecution je) {
        log.info("[{}] 시작 - {}",
                jobName,
                je.getStartTime() != null
                        ? je.getStartTime().toInstant().toString()
                        : "unknown");
    }

    @Override
    public void afterJob(JobExecution je) {
        long totalRead  = je.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum();
        long totalWrite = je.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum();
        long totalSkip  = je.getStepExecutions().stream().mapToLong(StepExecution::getSkipCount).sum();

        log.info("[{}] 종료 - 상태:{} read:{} write:{} skip:{}",
                jobName, je.getStatus(), totalRead, totalWrite, totalSkip);

        if (je.getStatus() == BatchStatus.FAILED) {
            log.error("[{}] 실패 원인: {}", jobName,
                    je.getAllFailureExceptions().isEmpty()
                            ? "알 수 없는 오류"
                            : je.getAllFailureExceptions().get(0).getMessage());
        }
    }

    /**
     * 실패 예외 메시지 추출 유틸
     */
    protected String extractErrorMsg(JobExecution je) {
        return je.getAllFailureExceptions().isEmpty()
                ? "알 수 없는 오류"
                : je.getAllFailureExceptions().get(0).getMessage();
    }

    /**
     * 총 처리 건수 합산 유틸
     */
    protected int extractTotalWrite(JobExecution je) {
        return (int) je.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount).sum();
    }
}