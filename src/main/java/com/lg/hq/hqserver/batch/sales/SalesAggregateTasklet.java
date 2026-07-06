package com.lg.hq.hqserver.batch.sales;

import com.lg.hq.hqserver.batch.common.support.BatchLogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class SalesAggregateTasklet {

    private static final Logger log = LoggerFactory.getLogger(SalesAggregateTasklet.class);

    private final JdbcTemplate hqJdbc;
    private final JdbcTemplate krJdbc;
    private final JdbcTemplate usJdbc;
    private final BatchLogSupport batchLog;

    public SalesAggregateTasklet(
            @Qualifier("hqJdbc") JdbcTemplate hqJdbc,
            @Qualifier("krJdbc") JdbcTemplate krJdbc,
            @Qualifier("usJdbc") JdbcTemplate usJdbc,
            BatchLogSupport batchLog) {
        this.hqJdbc   = hqJdbc;
        this.krJdbc   = krJdbc;
        this.usJdbc   = usJdbc;
        this.batchLog = batchLog;
    }

    public Tasklet krTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime started = LocalDateTime.now();
            int processed = aggregate("KR", krJdbc, "KRW", 1.0);
            batchLog.save("SALES_AGGREGATE", "KR", "SUCCESS", started, processed, null);
            log.info("[SalesAggregate] KR 완료 - {}건", processed);
            return RepeatStatus.FINISHED;
        };
    }

    public Tasklet usTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime started = LocalDateTime.now();
            Double usdRate = hqJdbc.queryForObject(
                    "SELECT rate_to_krw FROM exchange_rate " +
                            "WHERE currency_code='USD' AND rate_date=CURDATE()",
                    Double.class);
            if (usdRate == null) {
                log.warn("[SalesAggregate] USD 환율 없음 - 기본값 1320 사용");
                usdRate = 1320.0;
            }
            int processed = aggregate("US", usJdbc, "USD", usdRate);
            batchLog.save("SALES_AGGREGATE", "US", "SUCCESS", started, processed, null);
            log.info("[SalesAggregate] US 완료 - {}건", processed);
            return RepeatStatus.FINISHED;
        };
    }

    private int aggregate(String countryCode, JdbcTemplate jdbc,
                          String currency, Double rate) {
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT oi.product_code, " +
                        "SUM(oi.quantity) AS qty, " +
                        "SUM(oi.quantity * oi.unit_price) AS amount " +
                        "FROM order_items oi " +
                        "JOIN orders o ON oi.order_no = o.order_no " +
                        "WHERE o.status IN ('CONFIRMED','COMPLETED') " +
                        "AND o.order_date = CURDATE() " +
                        "GROUP BY oi.product_code");

        for (Map<String, Object> o : items) {
            double amountLocal = ((Number) o.get("amount")).doubleValue();
            hqJdbc.update(
                    "INSERT INTO fact_sales " +
                            "(country_code, product_code, sale_date, quantity, " +
                            "amount_local, currency_code, amount_krw, created_at) " +
                            "VALUES (?, ?, CURDATE(), ?, ?, ?, ?, NOW())",
                    countryCode,
                    o.get("product_code"),
                    ((Number) o.get("qty")).intValue(),
                    amountLocal,
                    currency,
                    amountLocal * rate);
        }
        return items.size();
    }
}