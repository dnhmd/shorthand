package com.shorthand.consumer.infrastructure.adapter.outbound.database;

import com.shorthand.consumer.domain.model.ClickMetric;
import com.shorthand.consumer.domain.port.outbound.ClickAnalyticsRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClickAnalyticsRepositoryAdapter implements ClickAnalyticsRepository {

    private final ClickAnalyticsJpaRepository clickAnalyticsJpaRepository;

    public ClickAnalyticsRepositoryAdapter(ClickAnalyticsJpaRepository clickAnalyticsJpaRepository) {
        this.clickAnalyticsJpaRepository = clickAnalyticsJpaRepository;
    }

    @Override
    public long totalClicks(String code) {
        return clickAnalyticsJpaRepository.countByLinkCode(code);
    }

    @Override
    public List<ClickMetric> getClicksByDate(String code) {
        return clickAnalyticsJpaRepository.countByDate(code);
    }

    @Override
    public List<ClickMetric> getClicksByCountry(String code) {
        return clickAnalyticsJpaRepository.countByCountry(code);
    }

    @Override
    public List<ClickMetric> getClicksByBrowser(String code) {
        return clickAnalyticsJpaRepository.countByBrowser(code);
    }

    @Override
    public List<ClickMetric> getClicksByOs(String code) {
        return clickAnalyticsJpaRepository.countByOs(code);
    }

    @Override
    public List<ClickMetric> getClicksByDevice(String code) {
        return clickAnalyticsJpaRepository.countByDevice(code);
    }
}
