package com.shorthand.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shorthand")
public record ShorthandProperties(
        LinkProperties link,
        SnowflakeProperties snowflake,
        KafkaProperties kafka,
        RateLimitProperties rateLimiter
) {
    public record LinkProperties(int defaultExpiryDays, String baseUrl) {}
    public record SnowflakeProperties(long machineId) {}
    public record KafkaProperties(String topic) {}
    public record RateLimitProperties(int capacity, double refillRate, String keyPrefix) {}
}
