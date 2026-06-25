package com.shorthand.backend.infrastructure.adapter.outbound.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shorthand.backend.domain.model.Link;
import com.shorthand.backend.domain.port.outbound.LinkCachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class LinkCacheAdapter implements LinkCachePort {

    private static final Logger log = LoggerFactory.getLogger(LinkCacheAdapter.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public LinkCacheAdapter(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Link> get(String code) {
        String json = redisTemplate.opsForValue().get(code);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, Link.class));
        } catch (JsonProcessingException ex) {
            log.error("Cache | Code: {} | Serialization Error (get)", code, ex);
            return Optional.empty();
        }
    }

    @Override
    public void put(String code, Link link, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(link);
            redisTemplate.opsForValue().set(code, json, ttl);
        } catch (JsonProcessingException ex) {
            log.error("Cache | Code: {} | Serialization Error (put)", code, ex);;
        }
    }
}
