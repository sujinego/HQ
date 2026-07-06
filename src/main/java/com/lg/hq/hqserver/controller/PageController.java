package com.lg.hq.hqserver.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class PageController {

    private final JdbcTemplate hqJdbc;

    public PageController(@Qualifier("hqJdbc") JdbcTemplate hqJdbc) {
        this.hqJdbc = hqJdbc;
    }

    @GetMapping("/")
    public String index() {
        return "index";   // → /WEB-INF/views/index.jsp
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/api/me")
    @ResponseBody
    public Map<String, Object> getCurrentUser() {
        var auth = org.springframework.security.core.context
                .SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("username", auth.getName());
        result.put("roles", auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(java.util.stream.Collectors.toList()));
        return result;
    }

    /**
     * 대용량 배치 테스트용 더미 상품 생성 (임시 엔드포인트)
     * 예: /seed-products?count=100000
     */
    @GetMapping("/seed-products")
    @ResponseBody
    public String seedProducts(@RequestParam(defaultValue = "100000") int count) {
        String sql =
                "INSERT INTO product_master (product_code, product_name_ko, product_name_en, " +
                        "category, base_cost, sale_price_krw, sale_price_usd, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

        String[] categories = {"NOTEBOOK", "MONITOR", "TV", "AC"};
        List<Object[]> batch = new ArrayList<>();
        int totalInserted = 0;

        for (int n = 1; n <= count; n++) {
            batch.add(new Object[]{
                    "TEST-" + String.format("%06d", n),
                    "테스트상품 " + n,
                    "Test Product " + n,
                    categories[n % 4],
                    10000 + (n % 500) * 1000,
                    15000 + (n % 500) * 1500,
                    12 + (n % 500),
                    "ACTIVE"
            });

            if (batch.size() == 5000) {
                hqJdbc.batchUpdate(sql, batch);
                totalInserted += batch.size();
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            hqJdbc.batchUpdate(sql, batch);
            totalInserted += batch.size();
        }

        return totalInserted + "건 생성 완료";
    }

    /**
     * 테스트 데이터 정리
     */
    @GetMapping("/clean-test-products")
    @ResponseBody
    public String cleanTestProducts() {
        int deleted = hqJdbc.update("DELETE FROM product_master WHERE product_code LIKE 'TEST-%'");
        return deleted + "건 삭제 완료";
    }

}
