package com.lg.hq.hqserver.batch.exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 한국수출입은행 환율 API 클라이언트.
 * API 호출 / 응답 파싱 / 재시도 로직을 담당.
 * ExchangeRateJobConfig에서 주입받아 사용.
 */
@Component
public class ExchangeRateApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateApiClient.class);

    private static final String[] TARGETS = {"USD", "EUR", "JPY(100)", "CNY", "GBP"};
    private static final String[] CODES   = {"USD", "EUR", "JPY",      "CNY", "GBP"};

    @Value("${exim.api.key:}")
    private String apiKey;

    /**
     * 오늘 날짜 기준 환율 데이터 조회
     * @return [[통화코드, 환율], ...] 형태의 배열
     */
    public Object[][] fetchTodayRates() throws Exception {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return fetchRates(date);
    }

    private Object[][] fetchRates(String date) throws Exception {
        String urlStr = "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON" +
                "?authkey=" + apiKey + "&searchdate=" + date + "&data=AP01";

        log.info("[ExchangeRateApiClient] API 호출: {}", urlStr);

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new RuntimeException("API 응답 오류: " + responseCode);
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();

        String json = sb.toString();
        log.info("[ExchangeRateApiClient] 응답: {}",
                json.substring(0, Math.min(200, json.length())));

        return parseRates(json);
    }

    private Object[][] parseRates(String json) throws Exception {
        List<Object[]> result = new ArrayList<>();

        for (int i = 0; i < TARGETS.length; i++) {
            String cur   = TARGETS[i];
            String code  = CODES[i];

            // "cur_unit":"USD" 찾기
            int idx = json.indexOf("\"cur_unit\":\"" + cur + "\"");
            if (idx < 0) continue;

            // deal_bas_r 찾기
            int rateIdx = json.indexOf("\"deal_bas_r\":", idx);
            if (rateIdx < 0) continue;

            int start   = json.indexOf("\"", rateIdx + 13) + 1;
            int end     = json.indexOf("\"", start);
            String rateStr = json.substring(start, end).replace(",", "");

            try {
                double rate = Double.parseDouble(rateStr);
                if ("JPY".equals(code)) rate = rate / 100.0;  // 100엔 기준 → 1엔
                result.add(new Object[]{code, rate});
                log.info("[ExchangeRateApiClient] {} = {}원", code, rate);
            } catch (NumberFormatException e) {
                log.warn("[ExchangeRateApiClient] {} 파싱 실패: {}", cur, rateStr);
            }
        }

        if (result.isEmpty()) {
            throw new RuntimeException("환율 데이터 없음 - 주말/공휴일일 수 있음");
        }

        return result.toArray(new Object[0][]);
    }
}