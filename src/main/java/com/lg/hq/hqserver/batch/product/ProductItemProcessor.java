package com.lg.hq.hqserver.batch.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

// ═══════════════════════════════════════════
// Processor: 가공 (검증/변환).스킵정책
// ═══════════════════════════════════════════

@Component
public class ProductItemProcessor
        implements ItemProcessor<Map<String, Object>, ProductItemProcessor.ProductSyncResult> {

    private static final Logger log = LoggerFactory.getLogger(ProductItemProcessor.class);

    @Override
    public ProductSyncResult process(Map<String, Object> item) {
        String code    = (String) item.get("product_code");
        String name    = (String) item.get("product_name_ko");
        Object priceKrw = item.get("sale_price_krw");
        Object priceUsd = item.get("sale_price_usd");

        if (code == null || code.isEmpty()) {
            log.warn("[ProductProcessor] product_code 누락 - skip");
            return null;
        }
        if (priceKrw == null && priceUsd == null) {
            log.warn("[ProductProcessor] {} 가격 정보 없음 - skip", code);
            return null;
        }

        return new ProductSyncResult(code, name, priceKrw, priceUsd);
    }

    // ── DTO ───────────────────────────────────
    public static class ProductSyncResult {
        public final String code;
        public final String name;
        public final Object priceKrw;
        public final Object priceUsd;

        public ProductSyncResult(String code, String name,
                                 Object priceKrw, Object priceUsd) {
            this.code     = code;
            this.name     = name;
            this.priceKrw = priceKrw;
            this.priceUsd = priceUsd;
        }
    }
}