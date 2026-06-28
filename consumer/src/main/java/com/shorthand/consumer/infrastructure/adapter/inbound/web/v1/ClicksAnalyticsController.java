package com.shorthand.consumer.infrastructure.adapter.inbound.web.v1;

import com.shorthand.consumer.domain.port.inbound.ClickAnalyticsUseCase;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.AnalyticsSummaryResponse;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.ClickMetricsResponse;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.TotalClicksResponse;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.mapper.ClicksAnalyticsWebMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/{code}")
public class ClicksAnalyticsController {

    private final ClickAnalyticsUseCase clickAnalyticsUseCase;
    private final ClicksAnalyticsWebMapper clicksAnalyticsWebMapper;

    public ClicksAnalyticsController(ClickAnalyticsUseCase clickAnalyticsUseCase, ClicksAnalyticsWebMapper clicksAnalyticsWebMapper) {
        this.clickAnalyticsUseCase = clickAnalyticsUseCase;
        this.clicksAnalyticsWebMapper = clicksAnalyticsWebMapper;
    }

    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> summary(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toSummaryResponse(
                        clickAnalyticsUseCase.getAnalyticsSummary(code)
                ));
    }

    @GetMapping("/clicks/total")
    public ResponseEntity<TotalClicksResponse> totalClicks(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toTotalClicksResponse(
                        clickAnalyticsUseCase.totalClicks(code)
                ));
    }

    @GetMapping("/clicks/by-date")
    public ResponseEntity<ClickMetricsResponse> getClicksByDate(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByDate(code)
                ));
    }

    @GetMapping("/clicks/by-country")
    public ResponseEntity<ClickMetricsResponse> getClicksByCountry(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByCountry(code)
                ));
    }

    @GetMapping("/clicks/by-browser")
    public ResponseEntity<ClickMetricsResponse> getClicksByBrowser(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByBrowser(code)
                ));
    }

    @GetMapping("/clicks/by-os")
    public ResponseEntity<ClickMetricsResponse> getClicksByOs(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByOs(code)
                ));
    }

    @GetMapping("/clicks/by-device")
    public ResponseEntity<ClickMetricsResponse> getClicksByDevice(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByDevice(code)
                ));
    }
}
