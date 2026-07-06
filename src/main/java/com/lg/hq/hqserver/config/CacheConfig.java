package com.lg.hq.hqserver.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {


    // 캐시 이름 상수
    public static final String KPI             = "kpi";
    public static final String EXCHANGE_RATES  = "exchangeRates";
    public static final String PRODUCTS        = "products";
    public static final String MONTHLY_SALES = "monthlySales";
    public static final String BATCH_SCHEDULES = "batchSchedules";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // 캐시 이름 명시적 등록 (이게 없으면 @Cacheable이 동작 안 함)
        manager.setCacheNames(java.util.List.of(KPI, EXCHANGE_RATES,PRODUCTS, MONTHLY_SALES));


        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)           // 최대 500개 항목
                .expireAfterWrite(10, TimeUnit.MINUTES)  // 10분 후 만료
                .recordStats());            // 캐시 통계 수집
        return manager;
    }
}