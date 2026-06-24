package com.shorthand.backend.infrastructure.adapter.outbound.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shorthand.backend.domain.port.outbound.LinkClickEventPublisherPort;
import com.shorthand.backend.infrastructure.config.ShorthandProperties;
import com.shorthand.common.event.LinkClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class LinkClickEventPublisherAdapter implements LinkClickEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LinkClickEventPublisherAdapter.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ShorthandProperties shorthandProperties;

    public LinkClickEventPublisherAdapter(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, ShorthandProperties shorthandProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.shorthandProperties = shorthandProperties;
    }

    @Async
    @Override
    public void publishMessage(LinkClickEvent event) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(event);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(shorthandProperties.kafka().topic(), event.code(), jsonMessage);
            future.whenCompleteAsync((result, ex) -> {
                if (ex == null) {
                    log.debug("LinkClickEvent Published: [Code: {}]", event.code());
                } else {
                    log.warn("LinkClickEvent Publish Failed: [Code: {}, Error: {}]", event.code(), ex.getMessage());
                }
            });
        } catch (JsonProcessingException ex) {
            log.error("JSON Serialization Failed: {}", ex.getMessage());
        }
    }
}
