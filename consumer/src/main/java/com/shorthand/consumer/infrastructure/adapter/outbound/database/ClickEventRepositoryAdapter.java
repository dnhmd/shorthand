package com.shorthand.consumer.infrastructure.adapter.outbound.database;

import com.shorthand.consumer.domain.model.ClickEvent;
import com.shorthand.consumer.domain.port.outbound.ClickEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ClickEventRepositoryAdapter implements ClickEventRepository {

    private final ClickEventJpaRepository clickEventJpaRepository;
    private final ClickEventEntityMapper clickEventEntityMapper;

    public ClickEventRepositoryAdapter(ClickEventJpaRepository clickEventJpaRepository, ClickEventEntityMapper clickEventEntityMapper) {
        this.clickEventJpaRepository = clickEventJpaRepository;
        this.clickEventEntityMapper = clickEventEntityMapper;
    }

    @Override
    public void save(ClickEvent clickEvent) {
        clickEventJpaRepository.saveAndFlush(clickEventEntityMapper.toEntity(clickEvent));
    }
}
