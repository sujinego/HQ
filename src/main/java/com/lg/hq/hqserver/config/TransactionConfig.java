package com.lg.hq.hqserver.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    @Bean(name = "hqTransactionManager")
    @Primary
    public PlatformTransactionManager hqTransactionManager(
            @Qualifier("hqDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public PlatformTransactionManager krTransactionManager(
            @Qualifier("krDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }

    @Bean
    public PlatformTransactionManager usTransactionManager(
            @Qualifier("usDataSource") DataSource ds) {
        return new DataSourceTransactionManager(ds);
    }


}