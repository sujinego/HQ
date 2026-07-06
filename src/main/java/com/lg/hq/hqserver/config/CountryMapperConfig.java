package com.lg.hq.hqserver.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(
        basePackages = "com.lg.hq.hqserver.mapper.country",
        sqlSessionFactoryRef = "countrySqlSessionFactory"
)
public class CountryMapperConfig {
}