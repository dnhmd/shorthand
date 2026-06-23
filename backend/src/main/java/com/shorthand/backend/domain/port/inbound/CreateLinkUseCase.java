package com.shorthand.backend.domain.port.inbound;

import com.shorthand.backend.domain.model.Link;

public interface CreateLinkUseCase {
    Link createLink(String originalUrl, Integer expiresInDays);
}
