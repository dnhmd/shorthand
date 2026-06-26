package com.shorthand.consumer.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shorthand")
public record ShorthandProperties(
        GeoIpProperties geoIp
) {
    public record GeoIpProperties(String databasePath) {}
}
