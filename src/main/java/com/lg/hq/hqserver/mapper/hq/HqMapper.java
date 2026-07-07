package com.lg.hq.hqserver.mapper.hq;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface HqMapper {

    // 배치 최신 이력 (단건)
    @Select("SELECT status, started_at, ended_at, records_processed, error_message " +
            "FROM batch_log WHERE batch_name = #{batchName} " +
            "ORDER BY log_id DESC LIMIT 1")
    Map<String, Object> findLastBatchLog(@Param("batchName") String batchName);

    // 국가별 배치 최신 이력
    @Select("SELECT b.country_code, b.status, b.started_at, b.ended_at, " +
            "b.records_processed, b.error_message " +
            "FROM batch_log b " +
            "INNER JOIN ( " +
            "  SELECT country_code, MAX(log_id) AS max_id " +
            "  FROM batch_log WHERE batch_name = #{batchName} " +
            "  GROUP BY country_code " +
            ") latest ON b.log_id = latest.max_id " +
            "ORDER BY b.country_code")
    List<Map<String, Object>> findLastBatchLogByCountry(@Param("batchName") String batchName);


    @Select("SELECT product_code, product_name_ko, status " +
            "FROM product_master WHERE status = #{status}")
    List<Map<String, Object>> findProductsByStatus(@Param("status") String status);

    @Select("SELECT * FROM exchange_rate WHERE rate_date = CURDATE() ORDER BY currency_code")
    List<Map<String, Object>> findTodayExchangeRates();

    @Select("SELECT * FROM batch_log ORDER BY started_at DESC LIMIT #{size} OFFSET #{offset}")
    List<Map<String, Object>> findBatchLogs(@Param("where") String where,
                                            @Param("size") int size,
                                            @Param("offset") int offset);


    // 국가별 마지막 상품 동기화 이력
    @Select("SELECT MAX(ended_at) AS last_sync, MAX(status) AS last_status " +
            "FROM batch_log " +
            "WHERE batch_name = 'LARGE_PRODUCT_SYNC' " +
            "AND (country_code = #{countryCode} OR country_code = 'ALL')")
    Map<String, Object> findLastProductSyncLog(@Param("countryCode") String countryCode);


    // 국가 목록 조회
    @Select("SELECT country_code, country_name, currency, is_active FROM country_ent ORDER BY country_code")
    List<Map<String, Object>> findAllCountries();

    // 국가 활성/비활성 토글
    @Update("UPDATE country_ent SET is_active = 1 - is_active WHERE country_code = #{countryCode}")
    int toggleCountryActive(@Param("countryCode") String countryCode);

    // 국가 활성 상태 조회
    @Select("SELECT is_active FROM country_ent WHERE country_code = #{countryCode}")
    Integer findCountryActiveStatus(@Param("countryCode") String countryCode);

    // HQ 상품 목록
    @Select("SELECT product_code, product_name_ko, status FROM product_master")
    List<Map<String, Object>> findAllProducts();


    @Select("SELECT f.country_code, e.country_name, " +
            "IFNULL(SUM(f.amount_krw), 0) AS total_krw, " +
            "IFNULL(SUM(f.quantity), 0) AS total_qty " +
            "FROM country_ent e " +
            "LEFT JOIN fact_sales f ON e.country_code = f.country_code " +
            "GROUP BY f.country_code, e.country_name")
    List<Map<String, Object>> findSalesByCountry();

    @Select("SELECT IFNULL(SUM(amount_krw), 0) FROM fact_sales " +
            "WHERE sale_date >= DATE_FORMAT(NOW(), '%Y-%m-01')")
    Double findMonthlySales();

    @Insert("INSERT INTO batch_log (batch_name, country_code, status, " +
            "started_at, ended_at, records_processed, error_message) " +
            "VALUES (#{batchName}, #{countryCode}, #{status}, " +
            "#{startedAt}, NOW(), #{records}, #{errorMsg})")
    void insertBatchLog(@Param("batchName") String batchName,
                        @Param("countryCode") String countryCode,
                        @Param("status") String status,
                        @Param("startedAt") java.time.LocalDateTime startedAt,
                        @Param("records") int records,
                        @Param("errorMsg") String errorMsg);

    @Select("SELECT COUNT(*) FROM batch_log ${where}")
    Integer countBatchLogs(@Param("where") String where);


    //배치 통계 (최근 7일)
    @Select("SELECT batch_name, " +
            "COUNT(*) AS total_runs, " +
            "SUM(CASE WHEN status='SUCCESS' THEN 1 ELSE 0 END) AS success_cnt, " +
            "SUM(CASE WHEN status='FAIL' THEN 1 ELSE 0 END) AS fail_cnt, " +
            "ROUND(AVG(TIMESTAMPDIFF(SECOND, started_at, ended_at)),1) AS avg_sec " +
            "FROM batch_log " +
            "WHERE started_at >= #{sinceDate} " +
//            "WHERE started_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY batch_name ORDER BY batch_name")
    List<Map<String, Object>> findBatchStats(@Param("sinceDate") LocalDateTime sinceDate);

    //감사로그
    @Select("<script>" +
            "SELECT * FROM audit_log WHERE 1=1 " +
            "<if test='action != null and action != \"\"'>AND action = #{action} </if>" +
            "<if test='entity != null and entity != \"\"'>AND entity = #{entity} </if>" +
            "ORDER BY created_at DESC LIMIT #{size}" +
            "</script>")
    List<Map<String, Object>> findAuditLogs(@Param("size") int size,
                                            @Param("action") String action,
                                            @Param("entity") String entity);

}