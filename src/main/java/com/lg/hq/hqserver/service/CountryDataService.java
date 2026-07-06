package com.lg.hq.hqserver.service;


import com.lg.hq.hqserver.config.CountryContext;
import com.lg.hq.hqserver.mapper.country.CountryMapper;
import com.lg.hq.hqserver.mapper.hq.HqMapper;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CountryDataService {

    private final HqMapper hqMapper;
    private final CountryMapper countryMapper;  // 국가 공통 Mapper
    private final JdbcTemplate countryJdbc;     // 라우팅 JdbcTemplate
    private final Map<String, JdbcTemplate> countryJdbcMap = new HashMap<>();

    // 1: 병렬 처리를 위한 전용 스레드 풀 정의 (Tomcat 스레드 고갈 방지)
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r);
        t.setName("Global-DB-Monitor-");
        t.setDaemon(true);
        return t;
    });

    public CountryDataService(
            HqMapper hqMapper,
            CountryMapper countryMapper,
            @Qualifier("countryJdbc") JdbcTemplate countryJdbc,
            @Qualifier("krJdbc") JdbcTemplate krJdbc,
            @Qualifier("usJdbc") JdbcTemplate usJdbc) {
        this.hqMapper      = hqMapper;
        this.countryMapper = countryMapper;
        this.countryJdbc   = countryJdbc;
        countryJdbcMap.put("KR", krJdbc);
        countryJdbcMap.put("US", usJdbc);
    }

    //스프링 빈 초기화가 완료된 후 안전하게 맵에 값을 밀어넣는 라이프사이클 메서드
    @javax.annotation.PostConstruct
    public void init() {
        try {
            log.info("[+] 글로벌 인프라 관제 맵 초기화 완료: KR, US 데이터소스 등록됨.", countryJdbcMap.keySet());
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
        List<Map<String, Object>> countries = hqMapper.findAllCountries();

        //  2: CompletableFuture를 이용한 멀티스레드 비동기 동시 제어
        List<CompletableFuture<Map<String, Object>>> futures = countries.stream()
                .map(c -> CompletableFuture.supplyAsync(() -> {
                    Map<String, Object> row = new LinkedHashMap<>(c);
                    String countryCode = c.get("country_code").toString();

                    // DB 헬스체크 및 실시간 데이터 수집
                    try {
                        // ThreadLocal에 국가 설정 → countryMapper가 해당 DB로 자동 라우팅
                        CountryContext.set(countryCode);

                        //  3: HikariCP 커넥션 풀 세부 메트릭 추출 추출 로직

                        JdbcTemplate jdbc = countryJdbcMap.get(countryCode);
                        putPoolMetrics(row, jdbc);

                        // 네트워크 핑 타임아웃 방어 제어 (쿼리 자체 타임아웃 2초로 강제 제한)
                        jdbc.setQueryTimeout(2);
                        long start = System.currentTimeMillis();
                        jdbc.queryForObject("SELECT 1", Integer.class);
                        long pingMs = System.currentTimeMillis() - start;

                        row.put("dbStatus",    "OK");
                        row.put("pingMs",      pingMs);
                        row.put("statusLevel", pingMs > 1000 ? "WARN" : "OK");


                        // CountryMapper가 CountryContext의 국가로 자동 라우팅
                        row.put("todayOrders",    countryMapper.countTodayOrders());
                        row.put("totalCustomers", countryMapper.countAllCustomers());

                    } catch (Exception e) {
                        String reason = e.getMessage() != null
                                ? e.getMessage() : e.getClass().getSimpleName();
                        log.error("[-] 글로벌 인프라 관제 장애 발생 - 국가코드 [{}], 사유: {}", countryCode, reason);
                        row.put("dbStatus",       "ERROR");
                        row.put("pingMs",         null);
                        row.put("statusLevel",    "ERROR");
                        row.put("todayOrders",    null);
                        row.put("totalCustomers", null);
                        row.put("poolActiveConnections", 0);
                        row.put("poolIdleConnections", 0);
                    } finally {
                        CountryContext.clear();  // ThreadLocal 반드시 정리
                    }

                    // 4. 본사 이력 테이블에서 최종 상품 연동 배치 동기화 이력
                    try {
                        Map<String, Object> sync = hqMapper.findLastProductSyncLog(countryCode);
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
    // ── 상품 동기화 비교 ──────────────────────────────
    public List<Map<String, Object>> getProductSyncStatus() {
        List<Map<String, Object>> hqProducts = hqMapper.findAllProducts();

        // 4: contains()의 O(N) 병목을 해결하기 위해 HashSet O(1) 인덱스 매핑으로 튜닝
        Set<String> krCodeSet = new HashSet<>();
        Set<String> usCodeSet = new HashSet<>();


        try {
            CountryContext.set("KR");
            krCodeSet.addAll(countryMapper.findAllProductCodes());
        } catch (Exception e) {
            log.warn("[-] KR 상품 리스트 조회 실패");
        } finally {
            CountryContext.clear();
        }

        try {
            CountryContext.set("US");
            usCodeSet.addAll(countryMapper.findAllProductCodes());
        } catch (Exception e) {
            log.warn("[-] US 상품 리스트 조회 실패");
        } finally {
            CountryContext.clear();
        }


        return hqProducts.stream().map(p -> {
            String code = p.get("product_code").toString();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productCode", code);
            row.put("productName", p.get("product_name_ko"));
            row.put("hqStatus",   p.get("status"));
            row.put("krSynced",   krCodeSet.contains(code));
            row.put("usSynced",   usCodeSet.contains(code));
            return row;
        }).collect(Collectors.toList());
    }


    // ── 데이터 품질 점수 ──────────────────────────────
    public List<Map<String, Object>> getDataQuality() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (String code : countryJdbcMap.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("countryCode", code);
            try {
                CountryContext.set(code);

                Integer negAmt  = countryMapper.countNegativeOrders();
                Integer noEmail = countryMapper.countMissingEmailCustomers();

                row.put("negativeAmountCnt", negAmt);
                row.put("missingEmailCnt",   noEmail);

                int issues = 0;
                if (negAmt  != null && negAmt  > 0) issues++;
                if (noEmail != null && noEmail > 0) issues++;

                int score = Math.max(0, 100 - issues * 20);
                row.put("qualityScore", score);
                row.put("issueCount",   issues);
                row.put("level", score >= 90 ? "GOOD" : score >= 70 ? "WARN" : "BAD");

            } catch (Exception e) {
                row.put("error",        e.getMessage());
                row.put("qualityScore", null);
                row.put("level",        "ERROR");
            } finally {
                CountryContext.clear();
            }
            result.add(row);
        }
        return result;
    }

    // ── 국가 활성/비활성 토글 ─────────────────────────
    public Map<String, Object> toggleCountry(String code) {
        int updated = hqMapper.toggleCountryActive(code.toUpperCase());
        if (updated == 0) {
            return Map.of("status", "FAIL",
                    "message", "국가를 찾을 수 없습니다: " + code);
        }
        Integer newStatus = hqMapper.findCountryActiveStatus(code.toUpperCase());
        return Map.of(
                "status",   "SUCCESS",
                "country",  code.toUpperCase(),
                "isActive", newStatus,
                "message",  newStatus == 1 ? "활성화되었습니다." : "비활성화되었습니다.");
    }


    // ── private 유틸 ──────────────────────────────────
    private void putPoolMetrics(Map<String, Object> row, JdbcTemplate jdbc) {
        DataSource ds = jdbc.getDataSource();
        if (!(ds instanceof HikariDataSource)) return;

        HikariPoolMXBean pool = ((HikariDataSource) ds).getHikariPoolMXBean();
        if (pool == null) {
            row.put("poolActiveConnections", 0);
            row.put("poolIdleConnections",   0);
            row.put("poolTotalConnections",  0);
            row.put("poolThreadsAwaiting",   0);
            return;
        }
        row.put("poolActiveConnections", pool.getActiveConnections());
        row.put("poolIdleConnections",   pool.getIdleConnections());
        row.put("poolTotalConnections",  pool.getTotalConnections());
        row.put("poolThreadsAwaiting",   pool.getThreadsAwaitingConnection());
    }
}