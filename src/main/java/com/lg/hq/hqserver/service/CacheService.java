package com.lg.hq.hqserver.service;

import com.lg.hq.hqserver.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final JdbcTemplate hqJdbc;

    public CacheService(
            @Qualifier("hqJdbc") JdbcTemplate hqJdbc) {
        this.hqJdbc = hqJdbc;
    }

    /**
     * 환율 캐시 (10분)
     * 환율 API는 하루 1번만 업데이트되므로 캐시 효과 극대화
     */
    @Cacheable(value = CacheConfig.EXCHANGE_RATES, key = "'today'")
    public List<Map<String, Object>> getExchangeRates() {
        log.info("[Cache MISS] exchangeRates - DB 조회");
        return hqJdbc.queryForList(
                "SELECT * FROM exchange_rate WHERE rate_date = CURDATE() ORDER BY currency_code");
    }

    /**
     * 상품 마스터 캐시 (10분)
     * 상품 정보는 자주 바뀌지 않음
     */
    @Cacheable(value = CacheConfig.PRODUCTS, key = "'active'")
    public List<Map<String, Object>> getActiveProducts() {
        log.info("[Cache MISS] products - DB 조회");
        return hqJdbc.queryForList(
                "SELECT * FROM product_master WHERE status = 'ACTIVE' ORDER BY product_code");
    }

    /**
     * KPI 캐시 (1분)
     * 대시보드 새로고침 시 DB 부하 감소
     */
    /**
     * 월 매출은 배치로 집계된 데이터 → 자주 안 바뀜 → 10분 캐시 적합
     */
    @Cacheable(value = CacheConfig.MONTHLY_SALES, key = "'current'")
    public Double getMonthlySalesCached() {
        log.info("[Cache MISS] monthlySales - DB 조회");
        try {
            LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
            Double sales = hqJdbc.queryForObject(
                    "SELECT IFNULL(SUM(amount_krw), 0) FROM fact_sales " +
                            "WHERE sale_date >= ?",
                    Double.class,
                    firstDayOfMonth);
            return sales != null ? sales : 0.0;
        } catch (Exception e) {
            log.warn("[Cache] monthlySales 조회 실패: {}", e.getMessage());
            return 0.0;
        }
    }

    /**
     * 월 매출 캐시 만료 (salesAggregateJob 완료 후 호출)
     */
    @CacheEvict(value = CacheConfig.MONTHLY_SALES, allEntries = true)
    public void evictMonthlySales() {
        log.info("[Cache EVICT] monthlySales 캐시 삭제");
    }

    /**
     * 캐시 무효화 - 환율 업데이트 후 호출
     */
    @CacheEvict(value = CacheConfig.EXCHANGE_RATES, allEntries = true)
    public void evictExchangeRates() {
        log.info("[Cache EVICT] exchangeRates 캐시 삭제");
    }

    /**
     * 캐시 무효화 - 상품 동기화 후 호출
     */
    @CacheEvict(value = CacheConfig.PRODUCTS, allEntries = true)
    public void evictProducts() {
        log.info("[Cache EVICT] products 캐시 삭제");
    }

    /**
     * 캐시 무효화 - 주문 등록/변경 후 호출
     */
    @CacheEvict(value = CacheConfig.KPI, allEntries = true)
    public void evictKpi() {
        log.info("[Cache EVICT] kpi 캐시 삭제");
    }

    /**
     * 전체 캐시 초기화
     */
    @CacheEvict(value = {
            CacheConfig.KPI,
            CacheConfig.EXCHANGE_RATES,
            CacheConfig.PRODUCTS,
            CacheConfig.BATCH_SCHEDULES
    }, allEntries = true)
    public void evictAll() {
        log.info("[Cache EVICT] 전체 캐시 삭제");
    }
}