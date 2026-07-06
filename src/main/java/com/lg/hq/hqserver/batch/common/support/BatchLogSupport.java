package com.lg.hq.hqserver.batch.common.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BatchLogSupport {
    private static final Logger log = LoggerFactory.getLogger(BatchLogSupport.class);
    private final JdbcTemplate hqJdbc;

    public BatchLogSupport(@Qualifier("hqJdbc") JdbcTemplate hqJdbc) {
        this.hqJdbc = hqJdbc;
    }

    /**
     * 배치 실행 이력 저장
     * @param batchName    배치 이름 (PRODUCT_SYNC, EXCHANGE_RATE 등)
     * @param countryCode  국가 코드 (KR, US, ALL 등) - 없으면 null
     * @param status       SUCCESS / FAIL / WARN
     * @param started      배치 시작 시각
     * @param records      처리 건수
     * @param errorMsg     오류 메시지 - 없으면 null
     */
    public void save(String batchName, String countryCode, String status,
                     LocalDateTime started, int records, String errorMsg) {
        try {
            hqJdbc.update(
                    "INSERT INTO batch_log " +
                            "(batch_name, country_code, status, started_at, ended_at, records_processed, error_message) " +
                            "VALUES (?, ?, ?, ?, NOW(), ?, ?)",
                    batchName, countryCode, status, started, records, errorMsg);
        } catch (Exception e) {
            log.error("[BatchLog] 저장 실패 - {} {}: {}", batchName, countryCode, e.getMessage());
        }
    }

    /**
     * 마지막 성공 실행 시각 조회 (Delta Sync 기준점)
     * @param batchName 배치 이름
     * @return 마지막 성공 시각 (없으면 1970-01-01 → 전체 처리)
     */
    public String getLastSuccessRunAt(String batchName) {
        try {
            String lastRun = hqJdbc.queryForObject(
                    "SELECT MAX(ended_at) FROM batch_log " +
                            "WHERE batch_name = ? AND status = 'SUCCESS'",
                    String.class, batchName);
            if (lastRun != null) return lastRun;
        } catch (Exception ignored) {}
        return "2026-01-01 00:00:00";
    }
}
