package com.lg.hq.hqserver.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.lg.hq.hqserver.mapper.hq.HqMapper;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class BatchMonitorService {

    private final HqMapper hqMapper;
    private final CacheManager cacheManager;


    private static final List<Map<String, String>> SCHEDULES = List.of(
            Map.of("batchName", "LARGE_PRODUCT_SYNC",    "jobKey", "large_product-sync",
                    "cron", "매 10분",    "desc", "HQ 상품마스터 → KR/US 동기화"),
            Map.of("batchName", "EXCHANGE_RATE",   "jobKey", "exchange-rate",
                    "cron", "매일 06:00", "desc", "환율 업데이트"),
            Map.of("batchName", "SALES_AGGREGATE", "jobKey", "sales-aggregate",
                    "cron", "매일 01:00", "desc", "국가별 매출 집계"),
            Map.of("batchName", "DATA_QUALITY",    "jobKey", "data-quality",
                    "cron", "매일 08:30", "desc", "데이터 품질 검증"),
            Map.of("batchName", "ATTENDANCE_CLOSING",  "jobKey", "attendance-closing",
                    "cron", "매일 00:00", "desc", "전일 근태 자동 마감"),
            Map.of("batchName", "PAYROLL_CALCULATE",   "jobKey", "payroll-calculate",
                    "cron", "매월 25일 09:00", "desc", "월 급여 자동 계산")
    );

    public BatchMonitorService(HqMapper hqMapper, CacheManager cacheManager) {
        this.hqMapper     = hqMapper;
        this.cacheManager = cacheManager;
    }

    /**
     * 배치 스케줄 현황 + 마지막 실행 결과
     * GET /api/batch/schedules
     */

    public List<Map<String, Object>> getSchedules() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, String> s : SCHEDULES) {
            String batchName = s.get("batchName");
            Map<String, Object> row = new LinkedHashMap<>(s);

            try {
                if ("SALES_AGGREGATE".equals(batchName)) {
                    List<Map<String, Object>> countryLogs = hqMapper.findLastBatchLogByCountry(batchName);

                    // 모든 country_code가 SUCCESS면 SUCCESS, 하나라도 아니면 FAIL
                    String overallStatus;
                    if (countryLogs.isEmpty()) {
                        overallStatus = "미실행";
                    } else {
                        boolean allSuccess = countryLogs.stream()
                                .allMatch(r -> "SUCCESS".equals(r.get("status")));
                        overallStatus = allSuccess ? "SUCCESS" : "FAIL";
                    }

                    row.put("lastStatus",  overallStatus);
                    row.put("lastStarted", countryLogs.isEmpty() ? null
                                        : countryLogs.get(0).get("started_at"));
                    row.put("lastRecords", countryLogs.stream().mapToInt(r -> {
                        Object v = r.get("records_processed");
                        return v == null ? 0 : ((Number) v).intValue();
                    }).sum());
                    row.put("lastError",    countryLogs.stream()
                            .filter(r -> !"SUCCESS".equals(r.get("status")))
                            .map(r -> r.get("country_code") + ": " + r.get("error_message"))
                            .findFirst()
                            .orElse(null));  // 실패한 country_code가 있으면 그 에러 메시지
                    row.put("countryLogs", countryLogs);

                } else {
                    Map<String, Object> last = hqMapper.findLastBatchLog(batchName);
                    row.put("lastStatus",  last.get("status"));
                    row.put("lastStarted", last.get("started_at"));
                    row.put("lastRecords", last.get("records_processed"));
                    row.put("lastError",   last.get("error_message"));
                }

            } catch (Exception e) {
                row.put("lastStatus",  "미실행");
                row.put("lastStarted", null);
                row.put("lastRecords", 0);
                row.put("lastError",   null);
            }

            result.add(row);
        }

        return result;
    }

    /**
     * 배치 실행 이력 조회
     * GET /api/batch/logs?page=0&size=20
     */

    public Map<String, Object> getBatchLogs(String batchName, String status, int page, int size) {

        StringBuilder where = new StringBuilder("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (batchName != null && !batchName.isEmpty()) {
            where.append("AND batch_name = '").append(batchName).append("' ");
        }
        if (status != null && !status.isEmpty()) {
            where.append("AND status = '").append(status).append("' ");
        }

        Integer total = hqMapper.countBatchLogs(where.toString());
        List<Map<String, Object>> logs = hqMapper.findBatchLogs(
                where.toString(), size, page * size);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("page",  page);
        result.put("size",  size);
        result.put("logs",  logs);
        return result;
    }


    /**
     * 배치 통계 (최근 7일)
     * GET /api/batch/stats
     */
    public List<Map<String, Object>> getBatchStats() {
        return hqMapper.findBatchStats();
    }

    /**
     * 로그 감사
     * POST /api/orders/{orderNo}/status
     */
    @GetMapping("/audit-logs")
    public List<Map<String, Object>> getAuditLogs(int size, String action, String entity) {
        return hqMapper.findAuditLogs(size, action, entity);
    }


    public Map<String, Object> getCacheStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        cacheManager.getCacheNames().forEach(name -> {
            org.springframework.cache.Cache cache  =  cacheManager.getCache(name);

            if (cache == null) return;

            Cache<Object, Object> nativeCache =
                        (Cache<Object, Object>) cache.getNativeCache();
                CacheStats stats = nativeCache.stats();

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("size",       nativeCache.estimatedSize());
                info.put("hitCount",   stats.hitCount());
                info.put("missCount",  stats.missCount());
                info.put("hitRate",    String.format("%.1f%%", stats.hitRate() * 100));
                result.put(name, info);

        });
        return result;
    }
//
//    @PostMapping("/run/large-product-sync")
//    public String runLargeProductSync() {
//        try {
//            org.springframework.batch.core.JobParameters params =
//                    new org.springframework.batch.core.JobParametersBuilder()
//                            .addLong("timestamp", System.currentTimeMillis())
//                            .toJobParameters();
//            jobLauncher.run(largeProductSyncJob, params);
//            return "대용량 상품 동기화 배치 실행 완료 (파티셔닝 4분할, chunk 1000)";
//        } catch (Exception e) {
//            return "실행 실패: " + e.getMessage();
//        }
//    }

}


