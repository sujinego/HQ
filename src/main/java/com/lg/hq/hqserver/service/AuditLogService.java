package com.lg.hq.hqserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

//감사 로그

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final JdbcTemplate hqJdbc;

    public AuditLogService(@Qualifier("hqJdbc") JdbcTemplate hqJdbc) {
        this.hqJdbc = hqJdbc;
    }

    /**
     * 비동기로 감사 로그 저장 (성능 영향 없음)
     */
    @Async
    public void log(String action, String entity, String entityId,
                    String beforeData, String afterData,
                    String userId, String ipAddress) {
        try {
            hqJdbc.update(
                    "INSERT INTO audit_log " +
                            "(action, entity, entity_id, before_data, after_data, user_id, ip_address, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())",
                    action, entity, entityId, beforeData, afterData,
                    userId != null ? userId : "SYSTEM",
                    ipAddress != null ? ipAddress : "unknown");
        } catch (Exception e) {
            log.error("[AuditLog] 저장 실패: {}", e.getMessage());
        }
    }

    // 편의 메서드들
    public void logCreate(String entity, String entityId, String afterData, String clientIp) {
        log("CREATE", entity, entityId, null, afterData, "SYSTEM", clientIp);
    }

    public void logUpdate(String entity, String entityId,
                          String beforeData, String afterData,  String changedBy, String clientIp) {
        log("UPDATE", entity, entityId, beforeData, afterData,  changedBy ,clientIp);
    }

    public void logDelete(String entity, String entityId, String beforeData,String changedBy, String clientIp) {
        log("DELETE", entity, entityId, beforeData, null ,changedBy , clientIp);
    }


    public void logBatch(String batchName, String status, int records) {
        log("BATCH_" + status, "BATCH", batchName,
                null, "records=" + records, "SYSTEM", null);
    }
}