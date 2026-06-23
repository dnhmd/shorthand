package com.shorthand.backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shorthand")
public record ShorthandProperties(
        LinkProperties link,
        SnowflakeProperties snowflake
) {
    public record LinkProperties(int defaultExpiryDays) {}
    public record SnowflakeProperties(long machineId) {}
}
