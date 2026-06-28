package com.shorthand.consumer.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.maxmind.geoip2.DatabaseReader;
import com.shorthand.consumer.application.service.ClickAnalyticsService;
import com.shorthand.consumer.application.service.ProcessClickEventService;
import com.shorthand.consumer.domain.port.inbound.ClickAnalyticsUseCase;
import com.shorthand.consumer.domain.port.inbound.ProcessClickEventUseCase;
import com.shorthand.consumer.domain.port.outbound.ClickAnalyticsRepository;
import com.shorthand.consumer.domain.port.outbound.ClickEventRepository;
import com.shorthand.consumer.infrastructure.adapter.outbound.geoip.GeoIpAdapter;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class ApplicationConfig {

    private static final int CACHE_SIZE = 1000;

    private final ShorthandProperties shorthandProperties;

    public ApplicationConfig(ShorthandProperties shorthandProperties) {
        this.shorthandProperties = shorthandProperties;
    }

    @Bean
    public UserAgentAnalyzer userAgentAnalyzer() {
        return UserAgentAnalyzer
                .newBuilder()
                .withCache(CACHE_SIZE)
                .build();
    }

    @Bean
    public ProcessClickEventUseCase processClickEventUseCase(UserAgentAnalyzer userAgentAnalyzer, GeoIpAdapter geoIpAdapter, ClickEventRepository clickEventRepository) {
        return new ProcessClickEventService(userAgentAnalyzer, geoIpAdapter, clickEventRepository);
    }

    @Bean
    public ClickAnalyticsUseCase clickAnalyticsUseCase(ClickAnalyticsRepository clickAnalyticsRepository) {
        return new ClickAnalyticsService(clickAnalyticsRepository);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public DatabaseReader databaseReader() throws IOException {
        File database = new File(shorthandProperties.geoIp().databasePath());
        return new DatabaseReader.Builder(database).build();
    }

    @Bean
    public GeoIpAdapter geoIpAdapter(DatabaseReader databaseReader) {
        return new GeoIpAdapter(databaseReader);
    }
}
