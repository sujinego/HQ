package com.lg.hq.hqserver.service;

import com.lg.hq.hqserver.batch.sales.SalesAggregateJobConfig;
import com.lg.hq.hqserver.config.CountryContext;
import com.lg.hq.hqserver.mapper.country.CountryMapper;
import com.lg.hq.hqserver.mapper.hq.HqMapper;
import com.lg.hq.hqserver.mapper.kr.KrMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);


    private final HqMapper hqMapper;
    private final CountryMapper countryMapper;
    private final CacheService cacheService;


    public DashboardService(HqMapper hqMapper, CountryMapper countryMapper, CacheService cacheService) {
        this.hqMapper = hqMapper;
        this.countryMapper = countryMapper;
        this.cacheService = cacheService;
    }
    public Map<String, Object> getKpi() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 이번달 매출: HQ합산 집계 ; 캐시 적용 (10분)
        result.put("monthlySalesKrw", cacheService.getMonthlySalesCached());

        // 실시간 지표: KR+US 합산:캐시 없이 직접 조회
        int todayOrders   = 0;
        int pendingCount = 0;
        int shippingCount = 0;

        for (String countryCode : List.of("KR", "US")) {
            try {
                CountryContext.set(countryCode);
                log.info("[DEBUG] CountryContext = {}", CountryContext.get());  // 추가

                todayOrders   += nvl(countryMapper.countTodayOrders());
                pendingCount += nvl(countryMapper.countOrdersByStatus("PENDING"));
                shippingCount += nvl(countryMapper.countOrdersByStatus("SHIPPING"));

                log.info("[DEBUG] {} → today:{} pending:{} shipping:{}",
                        countryCode, todayOrders, pendingCount, shippingCount);  // 추가

            }catch (Exception e) {
                log.warn("[Dashboard] 실시간 KPI 조회 실패: {}", e.getMessage());
            }finally {
                CountryContext.clear();
            }
        }

        result.put("todayOrders",   todayOrders);
        result.put("pendingCount", pendingCount);
        result.put("shippingCount", shippingCount);


        return result;
    }

    // null 방어
    private int nvl(Integer value) {
        return value != null ? value : 0;
    }

    public Map<String, Object> getRecentOrders() {
        Map<String, Object> result = new LinkedHashMap<>();

        for (String code : List.of("KR", "US")) {
            try {
                CountryContext.set(code);
                result.put(code, countryMapper.findRecentOrders(10));
            } catch (Exception e) {
                log.warn("[Dashboard] {} 최근 주문 조회 실패: {}", code, e.getMessage());
                result.put(code, List.of());
            } finally {
                CountryContext.clear();
            }
        }

        return result;
    }

    public List<Map<String, Object>> getSalesByCountry() {
        return hqMapper.findSalesByCountry();
    }
}