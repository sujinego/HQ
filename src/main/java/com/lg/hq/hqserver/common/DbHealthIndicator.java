package com.lg.hq.hqserver.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("multiDbHealth")
public class DbHealthIndicator implements HealthIndicator {

    private final JdbcTemplate hqJdbc;
    private final JdbcTemplate krJdbc;
    private final JdbcTemplate usJdbc;

    public DbHealthIndicator(
            @Qualifier("hqJdbc") JdbcTemplate hqJdbc,
            @Qualifier("krJdbc") JdbcTemplate krJdbc,
            @Qualifier("usJdbc") JdbcTemplate usJdbc) {
        this.hqJdbc = hqJdbc;
        this.krJdbc = krJdbc;
        this.usJdbc = usJdbc;
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        boolean allOk = true;

        // HQ DB 체크
        try {
            long start = System.currentTimeMillis();
            hqJdbc.queryForObject("SELECT 1", Integer.class);
            long ping = System.currentTimeMillis() - start;
            builder.withDetail("HQ_DB", "UP (ping: " + ping + "ms)");
        } catch (Exception e) {
            builder.withDetail("HQ_DB", "DOWN - " + e.getMessage());
            allOk = false;
        }

        // KR DB 체크
        try {
            long start = System.currentTimeMillis();
            krJdbc.queryForObject("SELECT 1", Integer.class);
            long ping = System.currentTimeMillis() - start;
            builder.withDetail("KR_DB", "UP (ping: " + ping + "ms)");
        } catch (Exception e) {
            builder.withDetail("KR_DB", "DOWN - " + e.getMessage());
            allOk = false;
        }

        // US DB 체크
        try {
            long start = System.currentTimeMillis();
            usJdbc.queryForObject("SELECT 1", Integer.class);
            long ping = System.currentTimeMillis() - start;
            builder.withDetail("US_DB", "UP (ping: " + ping + "ms)");
        } catch (Exception e) {
            builder.withDetail("US_DB", "DOWN - " + e.getMessage());
            allOk = false;
        }

        if (!allOk) builder.down();

        return builder.build();
    }
}