package com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.mapper;

import com.shorthand.consumer.domain.model.AnalyticsSummary;
import com.shorthand.consumer.domain.model.ClickMetric;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.AnalyticsSummaryResponse;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.ClickMetricsResponse;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.TotalClicksResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClicksAnalyticsWebMapper {

    public AnalyticsSummaryResponse toSummaryResponse(AnalyticsSummary analyticsSummary) {
        return new AnalyticsSummaryResponse(
                analyticsSummary.total(),
                analyticsSummary.dates(),
                analyticsSummary.countries(),
                analyticsSummary.browsers(),
                analyticsSummary.operatingSystems(),
                analyticsSummary.devices()
        );
    }

    public ClickMetricsResponse toClickMetricResponse(List<ClickMetric> clickMetrics) {
        return new ClickMetricsResponse(clickMetrics);
    }

    public TotalClicksResponse toTotalClicksResponse(long totalClicks) {
        return new TotalClicksResponse(totalClicks);
    }
}
