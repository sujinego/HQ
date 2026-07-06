package com.lg.hq.hqserver.batch.common.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * 모든 Step에 공통 적용되는 리스너.
 * 파티션 워커 Step에는 각 파티션 범위 정보를 추가 로깅.
 */
public class DefaultStepListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(DefaultStepListener.class);

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // 파티션 Step인 경우 범위 정보 출력
        String minCode = stepExecution.getExecutionContext().getString("minCode", "");
        String maxCode = stepExecution.getExecutionContext().getString("maxCode", "");

        if (!minCode.isEmpty()) {
            stepExecution.getExecutionContext()
                    .putLong("startMs", System.currentTimeMillis());
            log.info("[{}] 시작 - 범위: {} ~ {}",
                    stepExecution.getStepName(), minCode, maxCode);
        } else {
            log.info("[{}] 시작", stepExecution.getStepName());
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long startMs = stepExecution.getExecutionContext().getLong("startMs", 0L);
        long elapsed = startMs > 0 ? System.currentTimeMillis() - startMs : -1;
        int commitCount = stepExecution.getCommitCount();

        log.info("[{}] 종료 - read:{} write:{} skip:{} 소요:{}ms 커밋:{}회 청크평균:{}ms",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount(),
                elapsed,
                commitCount,
                commitCount > 0 && elapsed > 0 ? elapsed / commitCount : 0);

        return stepExecution.getExitStatus();
    }
}