package com.lg.hq.hqserver.batch.product;

import com.lg.hq.hqserver.batch.common.support.BatchLogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * product_code를 균등 분할하여 N개 파티션 생성
 * 실무에서는 ID range, hash mod, 날짜 range 등을 씀
 * 여기서는 product_code 알파벳 순으로 균등 분할
 */


@Component
public class ProductCodePartitioner implements Partitioner {

    private static final Logger log = LoggerFactory.getLogger(ProductCodePartitioner.class);

    private final JdbcTemplate hqJdbc;
    private final BatchLogSupport batchLogSupport;

    public ProductCodePartitioner(
            @Qualifier("hqJdbc") JdbcTemplate hqJdbc,
            BatchLogSupport batchLogSupport) {
        this.hqJdbc = hqJdbc;
        this.batchLogSupport = batchLogSupport;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        //1. 마지막 성공 실행 시각 조회
        String lastRunAt = batchLogSupport.getLastSuccessRunAt("LARGE_PRODUCT_SYNC");
        log.info("[ProductCodePartitioner] Delta Sync 기준 시각: {}", lastRunAt);

        //2.변경분만 조회
        List<String> changedCodes = hqJdbc.queryForList(
                "SELECT product_code FROM product_master " +
                        "WHERE status = 'ACTIVE' AND updated_at > ? " +
                        "ORDER BY product_code",
                String.class, lastRunAt);

        log.info("[ProductCodePartitioner] 변경 상품 수: {}건", changedCodes.size());

        Map<String, ExecutionContext> result = new HashMap<>();

        if (changedCodes.isEmpty()) {// 변경분 없음 → 빈 파티션 1개 (배치 즉시 완료)
            ExecutionContext ctx = new ExecutionContext();
            ctx.putString("minCode", "");
            ctx.putString("maxCode", "");
            ctx.putString("lastRunAt", lastRunAt);
            ctx.putInt("partitionIndex", 0);
            result.put("partition0", ctx);
            return result;
        }

        // 3. 변경된 코드를 gridSize로 균등 분할
        int total = changedCodes.size();
        int chunkSize = (int) Math.ceil((double) total / gridSize);

        for (int i = 0; i < gridSize; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, total);
            if (start >= end) break;

            ExecutionContext ctx = new ExecutionContext();
            ctx.putString("minCode", changedCodes.get(start));
            ctx.putString("maxCode", changedCodes.get(end - 1));
            ctx.putString("lastRunAt", lastRunAt);
            ctx.putInt("partitionIndex", i);
            result.put("partition" + i, ctx);
        }

        return result;
    }
}