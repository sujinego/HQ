//package com.lg.hq.hqserver.service;
//
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.batch.core.JobParameters;
//import org.springframework.batch.core.JobParametersBuilder;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class SyncBatchService_ {
//
//    private static final Logger log = LoggerFactory.getLogger(SyncBatchService_.class);
//
//
//    private final JdbcTemplate hqJdbc;
//    private final JdbcTemplate krJdbc;
//    private final JdbcTemplate usJdbc;
//
//    private final SlackNotificationService slack;
//    private final org.springframework.batch.core.launch.JobLauncher jobLauncher;
//    private final org.springframework.batch.core.Job largeProductSyncJob;
//
//    public SyncBatchService_(
//            @Qualifier("hqJdbc") JdbcTemplate hqJdbc,
//            @Qualifier("krJdbc") JdbcTemplate krJdbc,
//            @Qualifier("usJdbc") JdbcTemplate usJdbc,
//            SlackNotificationService slack
//            ) {
//        this.hqJdbc = hqJdbc;
//        this.krJdbc = krJdbc;
//        this.usJdbc = usJdbc;
//        this.slack = slack;
//    }
//
//    // ─────────────────────────────────────────────
//    // 1. 상품 동기화: HQ → KR/US (매 10분)
//    // 병목현상으로 교체 (2026.06.30 이수진)
//    // ─────────────────────────────────────────────
//    @Scheduled(fixedDelay = 600000)
//    public void runProductSync() {
//        try {
//            JobParameters params = new JobParametersBuilder()
//                    .addLong("timestamp", System.currentTimeMillis())
//                    .toJobParameters();
//            jobLauncher.run(largeProductSyncJob, params);
//        } catch (Exception e) {
//            log.error("[ProductSync] 실행 실패: {}", e.getMessage());
//            slack.sendBatchFail("PRODUCT_SYNC", e.getMessage(), 0);
//        }
//    }
////    @Scheduled(fixedDelay = 600000)
////    public void syncProducts() {
////        String batchName = "PRODUCT_SYNC";
////        LocalDateTime started = LocalDateTime.now();
////        log.info("[{}] 시작", batchName);
////        int processed = 0;
////
////        try {
////            List<Map<String, Object>> products = hqJdbc.queryForList(
////                    "SELECT product_code, product_name_ko, sale_price_krw, " +
////                            "sale_price_usd, status FROM product_master WHERE status = 'ACTIVE'");
////
////            for (Map<String, Object> p : products) {
////                String code = (String) p.get("product_code");
////                String name = (String) p.get("product_name_ko");
////                Object priceKrw = p.get("sale_price_krw");
////                Object priceUsd = p.get("sale_price_usd");
////
////                // KR 동기화
////                krJdbc.update(
////                        "INSERT INTO local_product_price " +
////                                "(product_code, product_name, sale_price, currency, synced_at) " +
////                                "VALUES (?, ?, ?, 'KRW', NOW()) " +
////                                "ON DUPLICATE KEY UPDATE " +
////                                "product_name=VALUES(product_name), " +
////                                "sale_price=VALUES(sale_price), synced_at=NOW()",
////                        code, name, priceKrw);
////
////                // US 동기화
////                usJdbc.update(
////                        "INSERT INTO local_product_price " +
////                                "(product_code, product_name, sale_price, currency, synced_at) " +
////                                "VALUES (?, ?, ?, 'USD', NOW()) " +
////                                "ON DUPLICATE KEY UPDATE " +
////                                "product_name=VALUES(product_name), " +
////                                "sale_price=VALUES(sale_price), synced_at=NOW()",
////                        code, name, priceUsd);
////
////                processed++;
////            }
////
////            saveBatchLog(batchName, null, "SUCCESS", started, processed, null);
////            log.info("[{}] 완료 - {}건", batchName, processed);
////
////        } catch (Exception e) {
////            saveBatchLog(batchName, null, "FAIL", started, processed, e.getMessage());
////            slack.sendBatchFail(batchName, e.getMessage(), processed);  // ← 추가
////            log.error("[{}] 실패: {}", batchName, e.getMessage());
////        }
////    }
//
//    // ─────────────────────────────────────────────
//    // 2. 환율 업데이트 (매일 06:00)
//    // ─────────────────────────────────────────────
//    @Scheduled(cron = "0 0 6 * * *")
//    public void updateExchangeRates() {
//        String batchName = "EXCHANGE_RATE";
//        LocalDateTime started = LocalDateTime.now();
//        log.info("[{}] 시작", batchName);
//
//        try {
//            // 실무에서는 외부 환율 API 호출. 여기선 고정값 사용
//            Object[][] rates = {
//                    {"USD", 1320.00},
//                    {"EUR", 1430.00},
//                    {"JPY", 8.80},
//                    {"CNY", 182.00},
//                    {"GBP", 1680.00}
//            };
//
//            int processed = 0;
//            for (Object[] rate : rates) {
//                hqJdbc.update(
//                        "INSERT INTO exchange_rate (rate_date, currency_code, rate_to_krw, created_at) " +
//                                "VALUES (CURDATE(), ?, ?, NOW()) " +
//                                "ON DUPLICATE KEY UPDATE rate_to_krw=VALUES(rate_to_krw), created_at=NOW()",
//                        rate[0], rate[1]);
//                processed++;
//            }
//
//            saveBatchLog(batchName, null, "SUCCESS", started, processed, null);
//            log.info("[{}] 완료 - {}건", batchName, processed);
//
//        } catch (Exception e) {
//            saveBatchLog(batchName, null, "FAIL", started, 0, e.getMessage());
//            log.error("[{}] 실패: {}", batchName, e.getMessage());
//        }
//    }
//
//    // ─────────────────────────────────────────────
//    // 3. 매출 집계: KR/US → HQ fact_sales (매일 01:00)
//    // ─────────────────────────────────────────────
//    @Scheduled(cron = "0 0 1 * * *")
//    public void aggregateSales() {
//        String batchName = "SALES_AGGREGATE";
//        LocalDateTime started = LocalDateTime.now();
//        log.info("[{}] 시작", batchName);
//        int processed = 0;
//
//        try {
//            // KR 집계
//            processed += aggregateCountrySales("KR", krJdbc, "KRW", 1.0);
//            // US 집계
//            Double usdRate = hqJdbc.queryForObject(
//                    "SELECT rate_to_krw FROM exchange_rate " +
//                            "WHERE currency_code='USD' AND rate_date=CURDATE()",
//                    Double.class);
//            if (usdRate == null) usdRate = 1320.0;
//            processed += aggregateCountrySales("US", usJdbc, "USD", usdRate);
//
//            saveBatchLog(batchName, null, "SUCCESS", started, processed, null);
//            log.info("[{}] 완료 - {}건", batchName, processed);
//
//        } catch (Exception e) {
//            saveBatchLog(batchName, null, "FAIL", started, processed, e.getMessage());
//            log.error("[{}] 실패: {}", batchName, e.getMessage());
//        }
//    }
//
//    private int aggregateCountrySales(String countryCode, JdbcTemplate jdbc,
//                                      String currency, Double rate) {
//        List<Map<String, Object>> orders = jdbc.queryForList(
//                "SELECT product_code, COUNT(*) AS qty, SUM(total_amount) AS amount " +
//                        "FROM orders WHERE status IN ('CONFIRMED','COMPLETED') " +
//                        "AND DATE(order_date) = CURDATE() " +
//                        "GROUP BY product_code");
//
//        for (Map<String, Object> o : orders) {
//            double amountLocal = ((Number) o.get("amount")).doubleValue();
//            double amountKrw = amountLocal * rate;
//
//            hqJdbc.update(
//                    "INSERT INTO fact_sales " +
//                            "(country_code, product_code, sale_date, quantity, " +
//                            "amount_local, currency_code, amount_krw, created_at) " +
//                            "VALUES (?, ?, CURDATE(), ?, ?, ?, ?, NOW())",
//                    countryCode,
//                    o.get("product_code"),
//                    ((Number) o.get("qty")).intValue(),
//                    amountLocal, currency, amountKrw);
//        }
//        return orders.size();
//    }
//
//    // ─────────────────────────────────────────────
//    // 4. 데이터 품질 검증 (매일 08:30)
//    // ─────────────────────────────────────────────
//    @Scheduled(cron = "0 30 8 * * *")
//    public void validateDataQuality() {
//        String batchName = "DATA_QUALITY";
//        LocalDateTime started = LocalDateTime.now();
//        log.info("[{}] 시작", batchName);
//        int issues = 0;
//
//        try {
//            // KR 음수 금액 체크
//            Integer krNeg = krJdbc.queryForObject(
//                    "SELECT COUNT(*) FROM orders WHERE total_amount < 0", Integer.class);
//            if (krNeg != null && krNeg > 0) {
//                log.warn("[DATA_QUALITY] KR 음수 금액 주문: {}건", krNeg);
//                issues += krNeg;
//            }
//
//            // KR 이메일 누락 체크
//            Integer krNoEmail = krJdbc.queryForObject(
//                    "SELECT COUNT(*) FROM customer WHERE contact_email IS NULL OR contact_email = ''",
//                    Integer.class);
//            if (krNoEmail != null && krNoEmail > 0) {
//                log.warn("[DATA_QUALITY] KR 이메일 누락 고객: {}건", krNoEmail);
//                issues += krNoEmail;
//            }
//
//            // US 음수 금액 체크
//            Integer usNeg = usJdbc.queryForObject(
//                    "SELECT COUNT(*) FROM orders WHERE total_amount < 0", Integer.class);
//            if (usNeg != null && usNeg > 0) {
//                log.warn("[DATA_QUALITY] US 음수 금액 주문: {}건", usNeg);
//                issues += usNeg;
//            }
//
//            String status = issues == 0 ? "SUCCESS" : "WARN";
//            saveBatchLog(batchName, null, status, started, issues,
//                    issues > 0 ? "품질 이슈 " + issues + "건 발견" : null);
//
//            if (issues > 0) {
//                slack.sendDataQualityWarn("KR/US", issues, "품질 이슈 " + issues + "건 발견");  // ← 추가
//            }
//
//            log.info("[{}] 완료 - 이슈 {}건", batchName, issues);
//
//        } catch (Exception e) {
//            saveBatchLog(batchName, null, "FAIL", started, 0, e.getMessage());
//            log.error("[{}] 실패: {}", batchName, e.getMessage());
//        }
//    }
//
//    // ─────────────────────────────────────────────
//    // 배치 로그 저장
//    // ─────────────────────────────────────────────
//    private void saveBatchLog(String batchName, String countryCode, String status,
//                              LocalDateTime started, int records, String errorMsg) {
//        try {
//            hqJdbc.update(
//                    "INSERT INTO batch_log " +
//                            "(batch_name, country_code, status, started_at, ended_at, " +
//                            "records_processed, error_message) " +
//                            "VALUES (?, ?, ?, ?, NOW(), ?, ?)",
//                    batchName, countryCode, status, started, records, errorMsg);
//        } catch (Exception e) {
//            log.error("배치 로그 저장 실패: {}", e.getMessage());
//        }
//    }
//
//    // ─────────────────────────────────────────────
//    // 수동 실행 메서드 (API에서 호출 가능)
//    // ─────────────────────────────────────────────
//    public String runManual(String jobKey) {
//        switch (jobKey) {
//            case "product-sync":
//                syncProducts();
//                return "상품 동기화 실행 완료";
//            case "exchange-rate":
//                updateExchangeRates();
//                return "환율 업데이트 실행 완료";
//            case "sales-aggregate":
//                aggregateSales();
//                return "매출 집계 실행 완료";
//            case "data-quality":
//                validateDataQuality();
//                return "데이터 품질 검증 실행 완료";
//            default:
//                return "알 수 없는 배치: " + jobKey;
//        }
//    }
//}