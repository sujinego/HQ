package com.lg.hq.hqserver.controller;

import com.lg.hq.hqserver.batch.common.runner.BatchManualRunner;
import com.lg.hq.hqserver.service.BatchMonitorService;
import com.lg.hq.hqserver.service.CacheService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchMonitorController {


    private final BatchMonitorService batchMonitorService;
    private final BatchManualRunner batchManualRunner;
    private final CacheService cacheService;

    public BatchMonitorController(
            BatchMonitorService batchMonitorService,
            BatchManualRunner batchManualRunner,
            CacheService cacheService) {
        this.batchMonitorService = batchMonitorService;
        this.batchManualRunner = batchManualRunner;
        this.cacheService = cacheService;
    }


    // ── 배치 수동 실행 ─────────────────────────
    @PostMapping("/run/{jobKey}")
    public String runManual(@PathVariable String jobKey) {
        return batchManualRunner.run(jobKey);
    }

    // ── 배치 스케줄 현황 + 마지막 실행 결과 ───
    @GetMapping("/schedules")
    public List<Map<String, Object>> getSchedules() {
        return batchMonitorService.getSchedules();
    }


    // ── 배치 실행 이력 조회 ────────────────────
    @GetMapping("/logs")
    public Map<String, Object> getBatchLogs(
            @RequestParam(required = false) String batchName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return batchMonitorService.getBatchLogs(batchName, status, page, size);
    }

    // ── 배치 통계 (최근 7일) ───────────────────
    @GetMapping("/stats")
    public List<Map<String, Object>> getBatchStats() {
        return batchMonitorService.getBatchStats();
    }

    // ── 환율 현황 ──────────────────────────────
    @GetMapping("/exchange-rates")
    public List<Map<String, Object>> getExchangeRates() {
        return cacheService.getExchangeRates();
    }

    // ── 감사 로그 ──────────────────────────────
    @GetMapping("/audit-logs")
    public List<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entity) {
        return batchMonitorService.getAuditLogs(size, action, entity);
    }

    // ── 캐시 통계 ──────────────────────────────
    @GetMapping("/cache-stats")
    public Map<String, Object> getCacheStats() {
        return batchMonitorService.getCacheStats();
    }



}
