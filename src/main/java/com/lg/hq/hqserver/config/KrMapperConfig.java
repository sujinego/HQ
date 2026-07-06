package com.lg.hq.hqserver.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(
        basePackages = "com.lg.hq.hqserver.mapper.kr",
        sqlSessionFactoryRef = "krSqlSessionFactory"
)
public class KrMapperConfig {
}
