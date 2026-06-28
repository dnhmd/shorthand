package com.shorthand.consumer.domain.model;

import java.util.List;

public record AnalyticsSummary(
        long total,
        List<ClickMetric> dates,
        List<ClickMetric> countries,
        List<ClickMetric> browsers,
        List<ClickMetric> operatingSystems,
        List<ClickMetric> devices
) {}
