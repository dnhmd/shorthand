package com.shorthand.backend.infrastructure.adapter.outbound.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkJpaRepository extends JpaRepository<LinkEntity, Long> {
    Optional<LinkEntity> findByCode(String code);
}
