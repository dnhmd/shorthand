package com.shorthand.consumer.infrastructure.adapter.inbound.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shorthand.common.event.LinkClickEvent;
import com.shorthand.consumer.domain.port.inbound.ProcessClickEventUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LinkClickEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LinkClickEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ProcessClickEventUseCase processClickEventUseCase;

    public LinkClickEventConsumer(ObjectMapper objectMapper, ProcessClickEventUseCase processClickEventUseCase) {
        this.objectMapper = objectMapper;
        this.processClickEventUseCase = processClickEventUseCase;
    }

    @KafkaListener(topics = "${shorthand.kafka.topic}")
    public void consume(String jsonLinkClickEvent) {
        try {
            LinkClickEvent linkClickEvent = objectMapper.readValue(jsonLinkClickEvent, LinkClickEvent.class);
            processClickEventUseCase.processClickEvent(linkClickEvent);
            log.info("Click Event Saved: [Code: {}]", linkClickEvent.code());
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize click event: {}", ex.getMessage());
        }
    }
}
