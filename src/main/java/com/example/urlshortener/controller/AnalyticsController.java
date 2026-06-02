package com.example.urlshortener.controller;

import com.example.urlshortener.dto.AnalyticsResponse;
import com.example.urlshortener.dto.ApiResponse;
import com.example.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for click analytics.
 *
 * Endpoint:
 *   GET /api/analytics/{code}
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /api/analytics/{code}
     *
     * Returns full analytics for a short code:
     * - Total clicks and unique clicks
     * - Daily breakdown (chronological)
     * - Top 10 referrers
     * - Browser/OS breakdown
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics(
            @PathVariable String code) {

        log.debug("Analytics request for code: {}", code);
        AnalyticsResponse analytics = analyticsService.getAnalytics(code);
        return ResponseEntity.ok(ApiResponse.ok(analytics));
    }
}
