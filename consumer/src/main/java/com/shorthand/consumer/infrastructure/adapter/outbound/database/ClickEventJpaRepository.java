package com.shorthand.consumer.infrastructure.adapter.outbound.database;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickEventJpaRepository extends JpaRepository<ClickEventEntity, Long> {
}
