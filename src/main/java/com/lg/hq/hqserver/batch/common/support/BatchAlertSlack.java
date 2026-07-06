package com.lg.hq.hqserver.batch.common.support;

import com.lg.hq.hqserver.service.SlackNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BatchAlertSlack {

    private static final Logger log = LoggerFactory.getLogger(BatchAlertSlack.class);
    private final SlackNotificationService slack;

    public BatchAlertSlack(SlackNotificationService slack) {
        this.slack = slack;
    }

    public void sendFail(String jobName, String errorMsg, int processed) {
        log.error("[BatchAlert] {} 실패 - 처리건수:{} 오류:{}", jobName, processed, errorMsg);
        slack.sendBatchFail(jobName, errorMsg, processed);
    }

    public void sendWarn(String jobName, String message) {
        log.warn("[BatchAlert] {} 경고 - {}", jobName, message);
    }
}
