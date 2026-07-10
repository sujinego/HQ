package com.lg.hq.hqserver.config;

import javax.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import java.util.TimeZone;

@Configuration
public class TimezoneConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.out.println("JVM 기본 타임존 설정: " + TimeZone.getDefault().getID());
    }
}