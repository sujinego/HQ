package com.lg.hq.hqserver.batch.product;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * StepScope Reader - 파티션마다 자기 범위(minCode~maxCode)만 페이징으로 읽음.
 * JdbcPagingItemReader는 LIMIT/OFFSET 방식이라 커넥션을 오래 잡지 않아
 * 대용량 + 멀티스레드 환경에 적합.
 *
 * 실제 운영에서는 @StepScope로 minCode/maxCode를 JobParameters에서 주입받지만,
 * 여기서는 파티션 ExecutionContext에서 가져오는 구조를 보여주기 위해
 * 팩토리 메서드 형태로 단순화함. 실제 적용 시 @StepScope + @Value("#{stepExecutionContext['minCode']}")
 * 형태로 주입하세요.
 */

@Configuration
public class ProductItemReader {

    private final DataSource hqBatchDataSource;

    @Value("${batch.chunk.size:1000}")
    private int chunkSize;

    public ProductItemReader(
            @Qualifier("hqBatchDataSource") DataSource hqBatchDataSource) {
        this.hqBatchDataSource = hqBatchDataSource;
    }

    @StepScope
    @Bean
    public JdbcPagingItemReader<Map<String, Object>> productReader(
            @Value("#{stepExecutionContext['minCode']}") String minCode,
            @Value("#{stepExecutionContext['maxCode']}") String maxCode,
            @Value("#{stepExecutionContext['lastRunAt']}") String lastRunAt,
            @Value("#{stepExecutionContext['partitionIndex']}") Integer partitionIndex) {

        JdbcPagingItemReader<Map<String, Object>> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(hqBatchDataSource);
        reader.setPageSize(chunkSize);
        reader.setRowMapper(new ColumnMapRowMapper());

        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause(
                "SELECT product_code, product_name_ko, sale_price_krw, sale_price_usd, status");
        queryProvider.setFromClause("FROM product_master");

        if (minCode == null || minCode.isEmpty()) {
            queryProvider.setWhereClause("WHERE 1=0");
        } else {
            queryProvider.setWhereClause(
                    "WHERE status = 'ACTIVE' " +
                            "AND updated_at > '" + lastRunAt + "' " +
                            "AND product_code BETWEEN '" + minCode + "' AND '" + maxCode + "'");
        }

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("product_code", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        reader.setQueryProvider(queryProvider);
        reader.setName("productReader-p" + (partitionIndex != null ? partitionIndex : 0));
        return reader;
    }
}