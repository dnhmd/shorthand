package com.shorthand.backend.infrastructure.adapter.outbound.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "links")
public class LinkEntity {

    @Id
    @Column(name = "id")
    private Long id;
    @Column(name = "code")
    private String code;
    @Column(name = "original_url")
    private String originalUrl;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "expires_at")
    private Instant expiresAt;

    public LinkEntity() {
    }

    public LinkEntity(Long id, String code, String originalUrl, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.code = code;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
