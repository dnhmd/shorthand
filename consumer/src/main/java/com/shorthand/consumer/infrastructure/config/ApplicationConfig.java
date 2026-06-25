package com.shorthand.consumer.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shorthand.consumer.application.service.ProcessClickEventService;
import com.shorthand.consumer.domain.port.inbound.ProcessClickEventUseCase;
import com.shorthand.consumer.domain.port.outbound.ClickEventRepository;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    private static final int CACHE_SIZE = 1000;

    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer
                .newBuilder()
                .withCache(CACHE_SIZE)
                .build();
    }

    @Bean
    public ProcessClickEventUseCase processClickEventUseCase(UserAgentAnalyzer userAgentAnalyzer, ClickEventRepository clickEventRepository) {
        return new ProcessClickEventService(userAgentAnalyzer, clickEventRepository);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
