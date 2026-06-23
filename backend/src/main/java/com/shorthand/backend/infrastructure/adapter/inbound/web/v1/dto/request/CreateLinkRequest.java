package com.shorthand.backend.infrastructure.adapter.inbound.web.v1.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateLinkRequest(
        @URL
        @NotBlank
        String originalLink,
        Integer expiresInDays
) {}
