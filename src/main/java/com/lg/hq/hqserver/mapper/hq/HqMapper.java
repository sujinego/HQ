package com.lg.hq.hqserver.mapper.hq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface HqMapper {

    // 배치 최신 이력 (단건)
    Map<String, Object> findLastBatchLog(@Param("batchName") String batchName);

    // 국가별 배치 최신 이력
    List<Map<String, Object>> findLastBatchLogByCountry(@Param("batchName") String batchName);

    // 상품 상태별 조회
    List<Map<String, Object>> findProductsByStatus(@Param("status") String status);

    // 오늘자 환율
    List<Map<String, Object>> findTodayExchangeRates();

    // 배치 로그 목록 (페이징)
    List<Map<String, Object>> findBatchLogs(@Param("where") String where,
                                            @Param("size") int size,
                                            @Param("offset") int offset);

    // 국가별 마지막 상품 동기화 이력
    Map<String, Object> findLastProductSyncLog(@Param("countryCode") String countryCode);

    // 국가 목록 조회
    List<Map<String, Object>> findAllCountries();

    // 국가 활성/비활성 토글
    int toggleCountryActive(@Param("countryCode") String countryCode);

    // 국가 활성 상태 조회
    Integer findCountryActiveStatus(@Param("countryCode") String countryCode);

    // HQ 상품 목록
    List<Map<String, Object>> findAllProducts();

    // 국가별 매출 합계
    List<Map<String, Object>> findSalesByCountry();

    // 이번 달 매출 합계
    Double findMonthlySales();

    // 배치 로그 등록
    void insertBatchLog(@Param("batchName") String batchName,
                        @Param("countryCode") String countryCode,
                        @Param("status") String status,
                        @Param("startedAt") LocalDateTime startedAt,
                        @Param("records") int records,
                        @Param("errorMsg") String errorMsg);

    // 배치 로그 총 건수
    Integer countBatchLogs(@Param("where") String where);

    // 배치 통계 (최근 7일)
    List<Map<String, Object>> findBatchStats(@Param("sinceDate") LocalDateTime sinceDate);

    // 감사 로그
    List<Map<String, Object>> findAuditLogs(@Param("size") int size,
                                            @Param("action") String action,
                                            @Param("entity") String entity);
}
