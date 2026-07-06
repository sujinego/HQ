package com.lg.hq.hqserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SlackNotificationService {

    private static final
    Logger log = LoggerFactory.getLogger(SlackNotificationService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    @Value("${slack.enabled:false}")
    private boolean enabled;

    /**
     * 배치 실패 알림
     */
    public void sendBatchFail(String batchName, String errorMsg, int records) {
        String msg = buildMessage(
                "*[HQ B2B] 배치 실패 알림*",
                "*배치명*: " + batchName,
                "*실패시간*: " + LocalDateTime.now().format(FMT),
                "*처리건수*: " + records + "건",
                "*오류내용*: ```" + truncate(errorMsg, 200) + "```"
        );
        send(msg);
    }

    /**
     * 배치 성공 알림 (선택적)
     */
    public void sendBatchSuccess(String batchName, int records) {
        String msg = buildMessage(
                "*[HQ B2B] 배치 완료*",
                "*배치명*: " + batchName,
                "*완료시간*: " + LocalDateTime.now().format(FMT),
                "*처리건수*: " + records + "건"
        );
        send(msg);
    }

    /**
     * 데이터 품질 경고
     */
    public void sendDataQualityWarn(String countryCode, int issueCount, String detail) {
        String msg = buildMessage(
                "*[HQ B2B] 데이터 품질 경고*",
                "*국가*: " + countryCode,
                "*발생시간*: " + LocalDateTime.now().format(FMT),
                "*이슈건수*: " + issueCount + "건",
                "*상세*: " + detail
        );
        send(msg);
    }

    /**
     * DB 연결 실패 알림
     */
    public void sendDbConnectionFail(String countryCode, String errorMsg) {
        String msg = buildMessage(
                "*[HQ B2B] DB 연결 실패*",
                "*국가DB*: " + countryCode,
                "*발생시간*: " + LocalDateTime.now().format(FMT),
                "*오류*: " + truncate(errorMsg, 200)
        );
        send(msg);
    }

    /**
     * 슬랙 메시지 전송
     */
    private void send(String message) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log.info("[Slack 미전송 - disabled] {}", message.substring(0, Math.min(50, message.length())));
            return;
        }

        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            String json = "{\"text\": " + toJson(message) + "}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                log.info("[Slack] 알림 전송 성공");
            } else {
                log.warn("[Slack] 전송 실패 - HTTP {}", code);
            }
            conn.disconnect();

        } catch (Exception e) {
            log.warn("[Slack] 전송 오류: {}", e.getMessage());
        }
    }

    private String buildMessage(String... lines) {
        return String.join("\n", lines);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String toJson(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                + "\"";
    }
}