package com.shorthand.backend.domain.model;

import java.time.Instant;

public record Link(
    Long id,
    String code,
    String originalUrl,
    Instant createdAt,
    Instant expiresAt
) {}
