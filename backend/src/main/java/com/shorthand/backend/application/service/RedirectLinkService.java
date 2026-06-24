package com.shorthand.backend.application.service;

import com.shorthand.backend.domain.exception.LinkExpiredException;
import com.shorthand.backend.domain.exception.LinkNotFoundException;
import com.shorthand.backend.domain.model.Link;
import com.shorthand.backend.domain.port.inbound.RedirectLinkUseCase;
import com.shorthand.backend.domain.port.outbound.LinkCachePort;
import com.shorthand.backend.domain.port.outbound.LinkClickEventPublisherPort;
import com.shorthand.backend.domain.port.outbound.LinkRepository;
import com.shorthand.common.event.LinkClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class RedirectLinkService implements RedirectLinkUseCase {

    private static final Logger log = LoggerFactory.getLogger(RedirectLinkService.class);

    private final LinkCachePort linkCachePort;
    private final LinkRepository linkRepository;
    private final LinkClickEventPublisherPort linkClickEventPublisherPort;

    public RedirectLinkService(LinkCachePort linkCachePort, LinkRepository linkRepository, LinkClickEventPublisherPort linkClickEventPublisherPort) {
        this.linkCachePort = linkCachePort;
        this.linkRepository = linkRepository;
        this.linkClickEventPublisherPort = linkClickEventPublisherPort;
    }

    @Override
    public String redirect(String code, String ipAddress, String userAgent, Instant now) {
        Optional<Link> cachedLink = linkCachePort.get(code);
        if (cachedLink.isPresent()) {
            Link link = cachedLink.get();
            checkExpiry(code, link);

            publishLinkClickEvent(code, ipAddress, userAgent, now);
            return link.originalUrl();
        } else {
            log.debug("Link redirection: [Code: {}, Status: Cache Miss]", code);
        }

        Optional<Link> dbLink = linkRepository.findByCode(code);
        if (dbLink.isPresent()) {
            Link link = dbLink.get();
            checkExpiry(code, link);
            Duration ttl = Duration.between(Instant.now(), link.expiresAt());
            linkCachePort.put(code, link, ttl);

            publishLinkClickEvent(code, ipAddress, userAgent, now);
            log.debug("Link redirection: [Code: {}, Status: Link in DB]", code);
            return link.originalUrl();
        }

        log.warn("Link redirection: [Code: {}, Status: Link Unavailable]", code);
        throw new LinkNotFoundException("Link Unavailable");
    }

    private void publishLinkClickEvent(String code, String ipAddress, String userAgent, Instant now) {
        linkClickEventPublisherPort.publishMessage(new LinkClickEvent(
                code,
                ipAddress,
                userAgent,
                now
        ));
    }

    private void checkExpiry(String code, Link link) {
        if (link.isExpired()) {
            log.warn("Link redirection: [Code: {}, Status: Link Expired]", code);
            throw new LinkExpiredException("Link Expired");
        }
    }
}
