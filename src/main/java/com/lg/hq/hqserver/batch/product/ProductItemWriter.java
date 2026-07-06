package com.lg.hq.hqserver.batch.product;

import com.lg.hq.hqserver.batch.product.ProductItemProcessor.ProductSyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// ═══════════════════════════════════════════
// Writer: KR/US 동시에 chunk 단위로 batch insert
// ═══════════════════════════════════════════

@Component
public class ProductItemWriter implements ItemWriter<ProductSyncResult> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemWriter.class);

    private static final String UPSERT_SQL =
            "INSERT INTO local_product_price (product_code, product_name, sale_price, synced_at) " +
                    "VALUES (?, ?, ?, NOW()) " +
                    "ON DUPLICATE KEY UPDATE product_name=?, sale_price=?, synced_at=NOW()";

    private final JdbcTemplate krBatchJdbc;
    private final JdbcTemplate usBatchJdbc;

    public ProductItemWriter(
            @Qualifier("krBatchJdbc") JdbcTemplate krBatchJdbc,
            @Qualifier("usBatchJdbc") JdbcTemplate usBatchJdbc) {
        this.krBatchJdbc = krBatchJdbc;
        this.usBatchJdbc = usBatchJdbc;
    }

    @Override
    public void write(List<? extends ProductSyncResult> items) {
        List<Object[]> krParams = new ArrayList<>();
        List<Object[]> usParams = new ArrayList<>();

        for (ProductSyncResult r : items) {
            krParams.add(new Object[]{r.code, r.name, r.priceKrw, r.name, r.priceKrw});
            usParams.add(new Object[]{r.code, r.name, r.priceUsd, r.name, r.priceUsd});
        }

        krBatchJdbc.batchUpdate(UPSERT_SQL, krParams);
        usBatchJdbc.batchUpdate(UPSERT_SQL, usParams);

        log.info("[ProductWriter] {}건 배치 INSERT 완료 (KR+US)", items.size());
    }
}