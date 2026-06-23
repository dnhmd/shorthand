package com.shorthand.backend.infrastructure.adapter.inbound.web.v1.mapper;

import com.shorthand.backend.domain.model.Link;
import com.shorthand.backend.infrastructure.adapter.inbound.web.v1.dto.response.CreateLinkResponse;
import com.shorthand.backend.infrastructure.config.ShorthandProperties;
import org.springframework.stereotype.Component;

@Component
public class LinkWebMapper {

    private final ShorthandProperties shorthandProperties;

    public LinkWebMapper(ShorthandProperties shorthandProperties) {
        this.shorthandProperties = shorthandProperties;
    }

    public CreateLinkResponse toResponse(Link link) {
        return new CreateLinkResponse(
                link.code(),
                shorthandProperties.link().baseUrl() + link.code(),
                link.originalUrl()
        );
    }
}
