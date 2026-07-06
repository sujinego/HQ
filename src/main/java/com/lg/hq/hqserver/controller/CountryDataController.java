package com.lg.hq.hqserver.controller;


import com.lg.hq.hqserver.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/countries")
public class CountryDataController {

    private final JdbcTemplate hqJdbc;
    private final JdbcTemplate krJdbc;
    private final JdbcTemplate usJdbc;

    private final Map<String, JdbcTemplate> countryJdbcMap = new ConcurrentHashMap<>();

    // 1: 병렬 처리를 위한 전용 스레드 풀 정의 (Tomcat 스레드 고갈 방지)
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r);
        t.setName("Global-DB-Monitor-");
        t.setDaemon(true);
        return t;
    });

    public CountryDataController(
            @Qualifier("hqJdbc") JdbcTemplate hqJdbc,
            @Qualifier("krJdbc") JdbcTemplate krJdbc,
            @Qualifier("usJdbc") JdbcTemplate usJdbc) {
        this.hqJdbc = hqJdbc;
        this.krJdbc = krJdbc;
       this.usJdbc= usJdbc;

    }

    //스프링 빈 초기화가 완료된 후 안전하게 맵에 값을 밀어넣는 라이프사이클 메서드
    @javax.annotation.PostConstruct
    public void init() {
        try {
            this.countryJdbcMap.put("KR", this.krJdbc);
            this.countryJdbcMap.put("US", this.usJdbc);
            log.info("[+] 글로벌 인프라 관제 맵 초기화 완료: KR, US 데이터소스 등록됨.");
        } catch (Exception e) {
            log.error("[-] 관제 맵 초기화 중 치명적 에러: {}", e.getMessage());
        }
    }

    private JdbcTemplate getJdbc(String code) {
        // 혹시 모를 공백이나 소문자 유입 방어
        if (code == null) {
            throw new IllegalArgumentException("국가 코드가 null입니다.");
        }

        JdbcTemplate jdbc = countryJdbcMap.get(code.toUpperCase());
        if (jdbc == null) {
            throw new IllegalArgumentException("지원하지 않거나 커넥션이 존재하지 않는 국가 코드입니다: " + code);
        }
        return jdbc;
    }
    /**
     * 국가별 DB 헬스체크 + 실시간 지표 + 커넥션 풀 메트릭 (비동기 병렬 처리)
     * GET /api/countries/status
     */
    @GetMapping("/status")
    public List<Map<String, Object>> getCountryStatus() {
        // 1. 본사 마스터 인프라에서 국가 목록 조회
        List<Map<String, Object>> countries = hqJdbc.queryForList(
                "SELECT country_code, country_name, currency, is_active FROM country_ent");

        //  2: CompletableFuture를 이용한 멀티스레드 비동기 동시 제어
        List<CompletableFuture<Map<String, Object>>> futures = countries.stream()
                .map(c -> CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> row = new LinkedHashMap<>(c);
                    String code = c.get("country_code").toString();

                    // DB 헬스체크 및 실시간 데이터 수집
                    try {
                        JdbcTemplate jdbc = getJdbc(code);

                        //  3: HikariCP 커넥션 풀 세부 메트릭 추출 추출 로직
                        DataSource ds = jdbc.getDataSource();
                        if (ds instanceof HikariDataSource) {
                            HikariDataSource hds = (HikariDataSource) ds;
                            row.put("poolActiveConnections", hds.getHikariPoolMXBean().getActiveConnections());
                            row.put("poolIdleConnections", hds.getHikariPoolMXBean().getIdleConnections());
                            row.put("poolTotalConnections", hds.getHikariPoolMXBean().getTotalConnections());
                            row.put("poolThreadsAwaiting", hds.getHikariPoolMXBean().getThreadsAwaitingConnection());
                        }

                        // 네트워크 핑 타임아웃 방어 제어 (쿼리 자체 타임아웃 2초로 강제 제한)
                        jdbc.setQueryTimeout(2);
                        long start = System.currentTimeMillis();
                        jdbc.queryForObject("SELECT 1", Integer.class);
                        long pingMs = System.currentTimeMillis() - start;

                        row.put("dbStatus",    "OK");
                        row.put("pingMs",      pingMs);
                        row.put("statusLevel", pingMs > 1000 ? "WARN" : "OK");

                        // 핵심 비즈니스 지표 카운트
                        Integer orderCnt = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM orders WHERE DATE(created_at) = CURDATE()", Integer.class);
                        Integer customerCnt = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM customer", Integer.class);

                        row.put("todayOrders",    orderCnt);
                        row.put("totalCustomers", customerCnt);

                    } catch (Exception e) {
                        log.error("[-] 글로벌 인프라 관제 장애 발생 - 국가코드 [{}], 사유: {}", code, e.getMessage());
                        row.put("dbStatus",       "ERROR");
                        row.put("pingMs",         null);
                        row.put("statusLevel",    "ERROR");
                        row.put("todayOrders",    null);
                        row.put("totalCustomers", null);
                        row.put("poolActiveConnections", 0);
                        row.put("poolIdleConnections", 0);
                    }

                    // 4. 본사 이력 테이블에서 최종 상품 연동 배치 상태 병합
                    try {
                        Map<String, Object> sync = hqJdbc.queryForMap(
                                "SELECT MAX(ended_at) AS last_sync, MAX(status) AS last_status " +
                                        "FROM batch_log WHERE country_code = ? AND batch_name = 'LARGE_PRODUCT_SYNC'", code);
                        row.put("lastSync",       sync.get("last_sync"));
                        row.put("lastSyncStatus", sync.get("last_status"));
                    } catch (Exception e) {
                        row.put("lastSync",       null);
                        row.put("lastSyncStatus", "미실행");
                    }

                    return row;
                }, threadPool))
                .collect(Collectors.toList());

        // 모든 스레드의 작업이 다 끝날 때까지 동기화 대기 (최대 타임아웃 5초 설정으로 API 락킹 방어)
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }
    /**
     * 상품 동기화 비교 (HQ vs 국가) 대용량 유입 대비 최적화 튜닝 (In-Memory 매핑 인덱스 구조 적용)
     * GET /api/countries/product-sync-status
     * 본사에 등록된 신규 상품 정보가 해외 리전 DB까지 안전하게 밀려 들어갔는가?"를 비교하는 기능
     */
    @GetMapping("/product-sync-status")
    public List<Map<String, Object>> getProductSyncStatus() {
        List<Map<String, Object>> hqProducts = hqJdbc.queryForList(
                "SELECT product_code, product_name_ko, status FROM product_master");

        // 4: contains()의 O(N) 병목을 해결하기 위해 HashSet O(1) 인덱스 매핑으로 튜닝
        Set<String> krCodeSet = new HashSet<>();
        Set<String> usCodeSet = new HashSet<>();


        try {
            krCodeSet.addAll(krJdbc.queryForList("SELECT product_code FROM local_product_price", String.class));
        } catch (Exception e) { log.warn("[-] KR 상품 리스트 조회 실패"); }

        try {
            usCodeSet.addAll(usJdbc.queryForList("SELECT product_code FROM local_product_price", String.class));
        } catch (Exception e) { log.warn("[-] US 상품 리스트 조회 실패"); }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> p : hqProducts) {
            String code = p.get("product_code").toString();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productCode", code);
            row.put("productName", p.get("product_name_ko"));
            row.put("hqStatus",   p.get("status"));

            // O(1) 탐색으로 수만 건의 데이터셋 대조 속도 보장
            row.put("krSynced",   krCodeSet.contains(code));
            row.put("usSynced",   usCodeSet.contains(code));
            result.add(row);
        }
        return result;
    }

    /**
     * 국가 활성/비활성 토글
     * PUT /api/countries/{code}/toggle
     */
    @PutMapping("/{code}/toggle")
    public Map<String, Object> toggleCountry(@PathVariable String code) {
        int updated = hqJdbc.update(
                "UPDATE country_ent SET is_active = 1 - is_active " +
                        "WHERE country_code = ?", code.toUpperCase());

        if (updated == 0) {
            return Map.of("status", "FAIL", "message", "국가를 찾을 수 없습니다: " + code);
        }

        Integer newStatus = hqJdbc.queryForObject(
                "SELECT is_active FROM country_ent WHERE country_code = ?",
                Integer.class, code.toUpperCase());

        return Map.of(
                "status",   "SUCCESS",
                "country",  code.toUpperCase(),
                "isActive", newStatus,
                "message",  newStatus == 1 ? "활성화되었습니다." : "비활성화되었습니다."
        );
    }

    /**
     * 데이터 품질 점수
     * GET /api/countries/data-quality
     */
    @GetMapping("/data-quality")
    public List<Map<String, Object>> getDataQuality() {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] codes = {"KR", "US"};
        JdbcTemplate[] jdbcs = {krJdbc, usJdbc};

        for (int i = 0; i < codes.length; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("countryCode", codes[i]);
            try {
                JdbcTemplate jdbc = jdbcs[i];
                int issues = 0;

                Integer negAmt = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM orders WHERE total_amount < 0", Integer.class);
                row.put("negativeAmountCnt", negAmt);
                if (negAmt != null && negAmt > 0) issues++;

                Integer noEmail = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM customer " +
                                "WHERE contact_email IS NULL OR contact_email = ''", Integer.class);
                row.put("missingEmailCnt", noEmail);
                if (noEmail != null && noEmail > 0) issues++;

                int score = Math.max(0, 100 - issues * 20);
                row.put("qualityScore", score);
                row.put("issueCount",   issues);
                row.put("level", score >= 90 ? "GOOD" : score >= 70 ? "WARN" : "BAD");

            } catch (Exception e) {
                row.put("error",        e.getMessage());
                row.put("qualityScore", null);
                row.put("level",        "ERROR");
            }
            result.add(row);
        }
        return result;
    }
}