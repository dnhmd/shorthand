package com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response;

import com.shorthand.consumer.domain.model.ClickMetric;

import java.util.List;

public record ClickMetricsResponse(List<ClickMetric> metrics) {}
