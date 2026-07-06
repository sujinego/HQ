package com.lg.hq.hqserver.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 요청 시점의 CountryContext에 따라 DataSource를 동적으로 결정.
 * determineCurrentLookupKey()가 반환하는 값으로 targetDataSources 맵에서 찾음.
 */
public class CountryRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return CountryContext.get();  // "KR", "US", "DE" 등
    }
}