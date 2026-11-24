package com.shiguangji.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * app.* 配置：上传目录、超时策略、JWT、业务上限。
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String uploadDir,
        Timeout timeout,
        Jwt jwt,
        Limits limits
) {

    public record Timeout(
            Duration orderPay,
            Duration autoConfirm,
            Duration reviewWindow,
            Duration aftersaleClaim,
            Duration aftersaleReply,
            Duration exchangeConfirm
    ) {
    }

    public record Jwt(String secret, Duration expiration) {
    }

    public record Limits(Integer rechargeMax, Integer maxOpenBounties, Integer bountyTolerance) {
    }
}
