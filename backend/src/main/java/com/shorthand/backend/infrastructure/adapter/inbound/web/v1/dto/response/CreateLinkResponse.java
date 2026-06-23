package com.shorthand.backend.infrastructure.adapter.inbound.web.v1.dto.response;

public record CreateLinkResponse(
        String code,
        String shortUrl,
        String originalLink
) {}
