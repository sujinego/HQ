package com.lg.hq.hqserver.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(
        basePackages = "com.lg.hq.hqserver.mapper.hq",
        sqlSessionFactoryRef = "hqSqlSessionFactory"
)
public class HqMapperConfig {
}
