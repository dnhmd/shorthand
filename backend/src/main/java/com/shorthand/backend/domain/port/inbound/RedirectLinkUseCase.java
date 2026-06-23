package com.shorthand.backend.domain.port.inbound;

public interface RedirectLinkUseCase {
    String redirect(String code);
}
