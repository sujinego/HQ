package com.lg.hq.hqserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Component
@Profile("demo")
public class DemoDataInitializer {

    private static final Logger log =
            LoggerFactory.getLogger(DemoDataInitializer.class);

    private final DataSource hqDataSource;
    private final DataSource krDataSource;
    private final DataSource usDataSource;

    public DemoDataInitializer(
            @Qualifier("hqDataSource") DataSource hqDataSource,
            @Qualifier("krDataSource") DataSource krDataSource,
            @Qualifier("usDataSource") DataSource usDataSource) {
        this.hqDataSource = hqDataSource;
        this.krDataSource = krDataSource;
        this.usDataSource = usDataSource;
    }

    @PostConstruct
    public void init() {

        // 이미 데이터가 있으면 스킵
        try {
            org.springframework.jdbc.core.JdbcTemplate hqJdbc =
                    new org.springframework.jdbc.core.JdbcTemplate(hqDataSource);
            Integer cnt = hqJdbc.queryForObject(
                    "SELECT COUNT(*) FROM portal_user", Integer.class);
            if (cnt != null && cnt > 0) {
                log.info("[DemoDataInitializer] 이미 초기화됨 - 스킵 (portal_user: {}건)", cnt);
                return;
            }
        } catch (Exception ignored) {
            // 테이블 없으면 초기화 진행
        }


        run(hqDataSource, "demo/hq-demo.sql");
        run(krDataSource, "demo/kr-demo.sql");
        run(usDataSource, "demo/us-demo.sql");

        log.info("[DemoDataInitializer] 데모 데이터 초기화 완료");



    }

    private void run(DataSource ds, String script) {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource(script));
            populator.setContinueOnError(true);
            populator.setSeparator(";");
            populator.setIgnoreFailedDrops(true);
            DatabasePopulatorUtils.execute(populator, ds);
            log.info("[DemoDataInitializer] {} 완료", script);
        } catch (Exception e) {
            log.error("[DemoDataInitializer] {} 실패: {}", script, e.getMessage());
        }
    }
}