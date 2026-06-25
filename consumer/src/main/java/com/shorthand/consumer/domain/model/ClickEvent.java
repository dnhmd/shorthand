package com.shorthand.consumer.domain.model;

import java.time.Instant;

public record ClickEvent(
        String linkCode,
        String ipAddress,
        String country,
        String device,
        String os,
        String browser,
        String userAgent,
        Instant clickedAt
) {}
