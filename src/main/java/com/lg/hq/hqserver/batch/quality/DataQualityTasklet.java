package com.lg.hq.hqserver.batch.quality;

import com.lg.hq.hqserver.batch.common.support.BatchAlertSlack;
import com.lg.hq.hqserver.batch.common.support.BatchLogSupport;
import com.lg.hq.hqserver.config.CountryContext;
import com.lg.hq.hqserver.mapper.country.CountryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataQualityTasklet {

    private static final Logger log = LoggerFactory.getLogger(DataQualityTasklet.class);

    private final CountryMapper   countryMapper;
    private final BatchLogSupport batchLog;
    private final BatchAlertSlack alert;

    @Value("${batch.target-countries}")
    private String targetCountries;

    public DataQualityTasklet(
            CountryMapper countryMapper,
            BatchLogSupport batchLog,
            BatchAlertSlack alert) {
        this.countryMapper = countryMapper;
        this.batchLog      = batchLog;
        this.alert         = alert;
    }

    public Tasklet qualityTasklet() {
        return (contribution, chunkContext) -> {
            List<String> codes = Arrays.asList(targetCountries.split(","));

            for (String code : codes) {
                LocalDateTime started = LocalDateTime.now();  // 루프 안에서 선언
                String countryCode    = code.trim().toUpperCase();
                int issues            = 0;
                StringBuilder detail  = new StringBuilder();

                try {
                    CountryContext.set(countryCode);  // 반드시 set 먼저

                    Integer negAmt  = countryMapper.countNegativeOrders();
                    Integer noEmail = countryMapper.countMissingEmailCustomers();

                    if (negAmt  != null && negAmt  > 0) {
                        issues += negAmt;
                        detail.append(countryCode).append(" 음수금액:").append(negAmt).append("건 ");
                    }
                    if (noEmail != null && noEmail > 0) {
                        issues += noEmail;
                        detail.append(countryCode).append(" 이메일누락:").append(noEmail).append("건 ");
                    }

                    String status = issues == 0 ? "SUCCESS" : "WARN";

                    batchLog.save("DATA_QUALITY", countryCode, status,
                            started, issues, issues > 0 ? detail.toString() : null);

                    if (issues > 0) {
                        alert.sendWarn("DATA_QUALITY",
                                "[" + countryCode + "] 이슈 " + issues + "건: " + detail);
                    }

                    log.info("[DataQuality] {} 완료 - 이슈 {}건", countryCode, issues);

                } catch (Exception e) {
                    // catch 블록에서 같은 스코프의 변수 사용 가능
                    batchLog.save("DATA_QUALITY", countryCode, "FAIL",
                            started, 0, e.getMessage());
                    log.error("[DataQuality] {} 실패: {}", countryCode, e.getMessage());

                } finally {
                    CountryContext.clear();  // 반드시 정리
                }
            }

            return RepeatStatus.FINISHED;
        };
    }
}