package com.shorthand.backend.infrastructure.adapter.outbound.database;

import com.shorthand.backend.domain.model.Link;
import com.shorthand.backend.domain.port.outbound.LinkRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LinkRepositoryAdapter implements LinkRepository {

    private final LinkJpaRepository linkJpaRepository;
    private final LinkEntityMapper linkEntityMapper;

    public LinkRepositoryAdapter(LinkJpaRepository linkJpaRepository, LinkEntityMapper linkEntityMapper) {
        this.linkJpaRepository = linkJpaRepository;
        this.linkEntityMapper = linkEntityMapper;
    }

    @Override
    public Optional<Link> findByCode(String code) {
        Optional<LinkEntity> linkEntityOptional = linkJpaRepository.findByCode(code);
        return linkEntityOptional.map(linkEntityMapper::toDomain);
    }

    @Override
    public void save(Link link) {
        linkJpaRepository.saveAndFlush(linkEntityMapper.toEntity(link));
    }
}
