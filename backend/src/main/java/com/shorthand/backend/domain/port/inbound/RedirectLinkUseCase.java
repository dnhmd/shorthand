package com.shorthand.backend.domain.port.inbound;

import java.time.Instant;

public interface RedirectLinkUseCase {
    String redirect(String code, String ipAddress, String userAgent, Instant now);
}
