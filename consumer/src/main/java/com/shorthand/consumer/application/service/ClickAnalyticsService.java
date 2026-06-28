package com.shorthand.consumer.application.service;

import com.shorthand.consumer.domain.model.AnalyticsSummary;
import com.shorthand.consumer.domain.model.ClickMetric;
import com.shorthand.consumer.domain.port.inbound.ClickAnalyticsUseCase;
import com.shorthand.consumer.domain.port.outbound.ClickAnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ClickAnalyticsService implements ClickAnalyticsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClickAnalyticsService.class);

    private final ClickAnalyticsRepository clickAnalyticsRepository;

    public ClickAnalyticsService(ClickAnalyticsRepository clickAnalyticsRepository) {
        this.clickAnalyticsRepository = clickAnalyticsRepository;
    }

    @Override
    public long totalClicks(String code) {
        log.debug("Analytics | Code: {} | Fetching Total Clicks", code);
        return clickAnalyticsRepository.totalClicks(code);
    }

    @Override
    public List<ClickMetric> getClicksByDate(String code) {
        log.debug("Analytics | Code: {} | Fetching By Date", code);
        return clickAnalyticsRepository.getClicksByDate(code);
    }

    @Override
    public List<ClickMetric> getClicksByCountry(String code) {
        log.debug("Analytics | Code: {} | Fetching By Country", code);
        return clickAnalyticsRepository.getClicksByCountry(code);
    }

    @Override
    public List<ClickMetric> getClicksByBrowser(String code) {
        log.debug("Analytics | Code: {} | Fetching By Browser", code);
        return clickAnalyticsRepository.getClicksByBrowser(code);
    }

    @Override
    public List<ClickMetric> getClicksByOs(String code) {
        log.debug("Analytics | Code: {} | Fetching By OS", code);
        return clickAnalyticsRepository.getClicksByOs(code);
    }

    @Override
    public List<ClickMetric> getClicksByDevice(String code) {
        log.debug("Analytics | Code: {} | Fetching By Device", code);
        return clickAnalyticsRepository.getClicksByDevice(code);
    }

    @Override
    public AnalyticsSummary getAnalyticsSummary(String code) {
        log.debug("Analytics | Code: {} | Fetching Summary", code);
        return new AnalyticsSummary(
                clickAnalyticsRepository.totalClicks(code),
                clickAnalyticsRepository.getClicksByDate(code),
                clickAnalyticsRepository.getClicksByCountry(code),
                clickAnalyticsRepository.getClicksByBrowser(code),
                clickAnalyticsRepository.getClicksByOs(code),
                clickAnalyticsRepository.getClicksByDevice(code)
        );
    }
}
