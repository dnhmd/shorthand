package com.shorthand.consumer.domain.port.inbound;

import com.shorthand.consumer.domain.model.AnalyticsSummary;
import com.shorthand.consumer.domain.model.ClickMetric;

import java.util.List;

public interface ClickAnalyticsUseCase {
    long totalClicks(String code);
    List<ClickMetric> getClicksByDate(String code);
    List<ClickMetric> getClicksByCountry(String code);
    List<ClickMetric> getClicksByBrowser(String code);
    List<ClickMetric> getClicksByOs(String code);
    List<ClickMetric> getClicksByDevice(String code);
    AnalyticsSummary getAnalyticsSummary(String code);
}
