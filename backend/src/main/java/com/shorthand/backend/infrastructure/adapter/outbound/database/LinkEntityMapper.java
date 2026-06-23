package com.shorthand.backend.infrastructure.adapter.outbound.database;

import com.shorthand.backend.domain.model.Link;
import org.springframework.stereotype.Component;

@Component
public class LinkEntityMapper {

    public LinkEntity toEntity(Link link) {
        LinkEntity linkEntity = new LinkEntity();
        linkEntity.setId(link.id());
        linkEntity.setCode(link.code());
        linkEntity.setOriginalUrl(link.originalUrl());
        linkEntity.setCreatedAt(link.createdAt());
        linkEntity.setExpiresAt(link.expiresAt());

        return linkEntity;
    }

    public Link toDomain(LinkEntity linkEntity) {
        return new Link(
                linkEntity.getId(),
                linkEntity.getCode(),
                linkEntity.getOriginalUrl(),
                linkEntity.getCreatedAt(),
                linkEntity.getExpiresAt()
        );
    }
}
