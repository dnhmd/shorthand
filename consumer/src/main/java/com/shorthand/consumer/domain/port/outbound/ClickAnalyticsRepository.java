package com.shorthand.consumer.domain.port.outbound;

import com.shorthand.consumer.domain.model.ClickMetric;

import java.util.List;

public interface ClickAnalyticsRepository {
    long totalClicks(String code);
    List<ClickMetric> getClicksByDate(String code);
    List<ClickMetric> getClicksByCountry(String code);
    List<ClickMetric> getClicksByBrowser(String code);
    List<ClickMetric> getClicksByOs(String code);
    List<ClickMetric> getClicksByDevice(String code);
}
