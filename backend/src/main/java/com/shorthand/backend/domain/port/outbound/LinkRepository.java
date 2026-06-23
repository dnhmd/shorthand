package com.shorthand.backend.domain.port.outbound;

import com.shorthand.backend.domain.model.Link;

import java.util.Optional;

public interface LinkRepository {
    Optional<Link> findByCode(String code);
    void save(Link link);
}
