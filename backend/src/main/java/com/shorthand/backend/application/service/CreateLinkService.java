package com.shorthand.backend.application.service;

import com.shorthand.backend.domain.model.Link;
import com.shorthand.backend.domain.port.inbound.CreateLinkUseCase;
import com.shorthand.backend.domain.port.outbound.LinkIdentifierPort;
import com.shorthand.backend.domain.port.outbound.LinkRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class CreateLinkService implements CreateLinkUseCase {

    private final LinkRepository linkRepository;
    private final LinkIdentifierPort linkIdentifierPort;
    private final int defaultExpiryDays;

    public CreateLinkService(LinkRepository linkRepository, LinkIdentifierPort linkIdentifierPort, int defaultExpiryDays) {
        this.linkRepository = linkRepository;
        this.linkIdentifierPort = linkIdentifierPort;
        this.defaultExpiryDays = defaultExpiryDays;
    }

    @Override
    public Link createLink(String originalUrl, Integer expiresInDays) {
        Long id = linkIdentifierPort.generateSnowflakeId();
        String code = linkIdentifierPort.generateCode();

        if (expiresInDays == null) expiresInDays = defaultExpiryDays;

        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(expiresInDays, ChronoUnit.DAYS);

        Link link = new Link(id, code, originalUrl, createdAt, expiresAt);
        linkRepository.save(link);

        return link;
    }
}
