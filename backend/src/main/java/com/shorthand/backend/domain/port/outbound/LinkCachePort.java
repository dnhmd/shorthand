package com.shorthand.backend.domain.port.outbound;

import com.shorthand.backend.domain.model.Link;

import java.time.Duration;
import java.util.Optional;

public interface LinkCachePort {
    Optional<Link> get(String code);
    void put(String code, Link link, Duration ttl);
}
