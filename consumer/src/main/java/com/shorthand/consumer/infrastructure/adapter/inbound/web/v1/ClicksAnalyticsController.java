package com.shorthand.consumer.infrastructure.adapter.inbound.web.v1;

import com.shorthand.consumer.domain.port.inbound.ClickAnalyticsUseCase;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.AnalyticsSummaryResponse;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.ClickMetricsResponse;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.dto.response.TotalClicksResponse;
import com.shorthand.consumer.infrastructure.adapter.inbound.web.v1.mapper.ClicksAnalyticsWebMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analytics", description = "Click analytics and metrics for short links")
@RestController
@RequestMapping("/api/v1/analytics/{code}")
public class ClicksAnalyticsController {

    private final ClickAnalyticsUseCase clickAnalyticsUseCase;
    private final ClicksAnalyticsWebMapper clicksAnalyticsWebMapper;

    public ClicksAnalyticsController(ClickAnalyticsUseCase clickAnalyticsUseCase, ClicksAnalyticsWebMapper clicksAnalyticsWebMapper) {
        this.clickAnalyticsUseCase = clickAnalyticsUseCase;
        this.clicksAnalyticsWebMapper = clicksAnalyticsWebMapper;
    }

    @Operation(summary = "Get Analytics Summary", description = "Returns total clicks, and breakdowns by date, country, browser, OS, and device")
    @GetMapping("/summary")
    public ResponseEntity<AnalyticsSummaryResponse> summary(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toSummaryResponse(
                        clickAnalyticsUseCase.getAnalyticsSummary(code)
                ));
    }

    @Operation(summary = "Total Clicks", description = "Returns the total number of clicks for a short link")
    @GetMapping("/clicks/total")
    public ResponseEntity<TotalClicksResponse> totalClicks(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toTotalClicksResponse(
                        clickAnalyticsUseCase.totalClicks(code)
                ));
    }

    @Operation(summary = "Clicks by Date", description = "Returns click counts grouped by day")
    @GetMapping("/clicks/by-date")
    public ResponseEntity<ClickMetricsResponse> getClicksByDate(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByDate(code)
                ));
    }

    @Operation(summary = "Clicks by Country", description = "Returns click counts grouped by country")
    @GetMapping("/clicks/by-country")
    public ResponseEntity<ClickMetricsResponse> getClicksByCountry(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByCountry(code)
                ));
    }

    @Operation(summary = "Clicks by Browser", description = "Returns click counts grouped by browser")
    @GetMapping("/clicks/by-browser")
    public ResponseEntity<ClickMetricsResponse> getClicksByBrowser(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByBrowser(code)
                ));
    }

    @Operation(summary = "Clicks by OS", description = "Returns click counts grouped by operating system")
    @GetMapping("/clicks/by-os")
    public ResponseEntity<ClickMetricsResponse> getClicksByOs(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByOs(code)
                ));
    }

    @Operation(summary = "Clicks by Device", description = "Returns click counts grouped by device type")
    @GetMapping("/clicks/by-device")
    public ResponseEntity<ClickMetricsResponse> getClicksByDevice(@PathVariable("code") String code) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(clicksAnalyticsWebMapper.toClickMetricResponse(
                        clickAnalyticsUseCase.getClicksByDevice(code)
                ));
    }
}
