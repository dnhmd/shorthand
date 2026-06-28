package com.shorthand.consumer.infrastructure.adapter.inbound.web.exception;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        Integer status,
        String error,
        String message
) {}
