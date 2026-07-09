package com.lg.hq.hqserver.config;


import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {
    //화면용 (MyBatis + JdbcTemplate):  hqDataSource  → hqJdbc, hqSqlSessionFactory
    //배치 전용 (JdbcTemplate만): hqBatchDataSource → hqBatchJdbc   ← HQ Reader용
    // krBatchDataSource → krBatchJdbc   ← KR Writer용

    // XML 매퍼 경로 패턴 (resources/mapper/** 하위 전부)
    private static final String MAPPER_LOCATION_PATTERN = "classpath:mapper/**/*.xml";

    // ── DataSource ────────────────────────
    @Bean @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hq")
    public DataSource hqDataSource() { return DataSourceBuilder.create().build(); }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.kr")
    public DataSource krDataSource() { return DataSourceBuilder.create().build(); }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.us")
    public DataSource usDataSource() { return DataSourceBuilder.create().build(); }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hq-batch")
    public DataSource hqBatchDataSource() { return DataSourceBuilder.create().build(); }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.kr-batch")
    public DataSource krBatchDataSource() { return DataSourceBuilder.create().build(); }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.us-batch")
    public DataSource usBatchDataSource() { return DataSourceBuilder.create().build(); }

    // ── CountryRoutingDataSource (신규) ──────────────
    @Bean
    public DataSource countryRoutingDataSource(
            @Qualifier("krDataSource") DataSource krDataSource,
            @Qualifier("usDataSource") DataSource usDataSource) {

        CountryRoutingDataSource routing = new CountryRoutingDataSource();

        // 국가 추가 시 여기만 수정
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("KR", krDataSource);
        targetDataSources.put("US", usDataSource);
        // targetDataSources.put("DE", deDataSource);  ← 독일 추가 시 이렇게
        // targetDataSources.put("CN", cnDataSource);  ← 중국 추가 시 이렇게

        routing.setTargetDataSources(targetDataSources);
        routing.setDefaultTargetDataSource(krDataSource);  // 기본값
        routing.afterPropertiesSet();
        return routing;
    }

    // ── CountryMapper 전용 SqlSessionFactory ─────────
    @Bean
    public SqlSessionFactory countrySqlSessionFactory(
            @Qualifier("countryRoutingDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        // XML 매퍼 위치 명시 (mapper/country/*.xml 포함)
        f.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION_PATTERN)
        );
        // MyBatis 설정 추가
        org.apache.ibatis.session.Configuration config =
                new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(config);
        return f.getObject();
    }

    // ── SqlSessionFactory ─────────────────
    @Bean @Primary
    public SqlSessionFactory hqSqlSessionFactory(
            @Qualifier("hqDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        // XML 매퍼 위치 명시 (mapper/hq/*.xml 포함)
        f.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION_PATTERN)
        );
        // MyBatis 설정 추가
        org.apache.ibatis.session.Configuration config =
                new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(config);
        return f.getObject();
    }

    @Bean
    public SqlSessionFactory krSqlSessionFactory(
            @Qualifier("krDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        f.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION_PATTERN)
        );
        // MyBatis 설정 추가
        org.apache.ibatis.session.Configuration config =
                new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(config);
        return f.getObject();
    }

    @Bean
    public SqlSessionFactory usSqlSessionFactory(
            @Qualifier("usDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean f = new SqlSessionFactoryBean();
        f.setDataSource(ds);
        f.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources(MAPPER_LOCATION_PATTERN)
        );
        // MyBatis 설정 추가
        org.apache.ibatis.session.Configuration config =
                new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        f.setConfiguration(config);
        return f.getObject();
    }

    // ── JdbcTemplate (화면용) ──────────────────────
    @Bean
    public JdbcTemplate hqJdbc(@Qualifier("hqDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public JdbcTemplate krJdbc(@Qualifier("krDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public JdbcTemplate usJdbc(@Qualifier("usDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    // ── JdbcTemplate (배치 전용) ──────────
    @Bean
    public JdbcTemplate hqBatchJdbc(@Qualifier("hqBatchDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public JdbcTemplate krBatchJdbc(@Qualifier("krBatchDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    @Bean
    public JdbcTemplate usBatchJdbc(@Qualifier("usBatchDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
    @Bean
    public JdbcTemplate countryJdbc(
            @Qualifier("countryRoutingDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

}
