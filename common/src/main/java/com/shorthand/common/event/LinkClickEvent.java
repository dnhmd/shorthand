package com.shorthand.common.event;

import java.time.Instant;

public record LinkClickEvent(
        String code,
        String ipAddress,
        String userAgent,
        Instant clickedAt
) {}
