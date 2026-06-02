package com.example.urlshortener.service;

import com.example.urlshortener.dto.AnalyticsResponse;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.ClickEventRepository;
import com.example.urlshortener.repository.UrlMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates click analytics for a given short code.
 *
 * All queries leverage the composite index on (url_mapping_id, timestamp)
 * in click_events to avoid full-table scans.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsService {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;

    /**
     * Build a full analytics report for the given short code.
     *
     * @param code the short code or custom alias
     * @return AnalyticsResponse with all aggregated stats
     */
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String code) {
        UrlMapping mapping = urlMappingRepository.findByCodeOrCustomAlias(code)
                .orElseThrow(() -> new UrlNotFoundException(code));

        long totalClicks = mapping.getClicks();  // fast denormalized read
        long uniqueClicks = clickEventRepository.countDistinctIpByUrlMapping(mapping);

        List<AnalyticsResponse.DailyStat> daily = buildDailyStats(mapping.getId());
        List<AnalyticsResponse.ReferrerStat> referrers = buildReferrerStats(mapping.getId());
        Map<String, Long> browsers = buildBrowserStats(mapping.getId());

        return AnalyticsResponse.builder()
                .code(mapping.getCode())
                .longUrl(mapping.getLongUrl())
                .totalClicks(totalClicks)
                .uniqueClicks(uniqueClicks)
                .dailyBreakdown(daily)
                .topReferrers(referrers)
                .browserBreakdown(browsers)
                .build();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private List<AnalyticsResponse.DailyStat> buildDailyStats(Long mappingId) {
        return clickEventRepository.findDailyClickCounts(mappingId)
                .stream()
                .map(row -> AnalyticsResponse.DailyStat.builder()
                        .date(String.valueOf(row[0]))
                        .clicks(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    private List<AnalyticsResponse.ReferrerStat> buildReferrerStats(Long mappingId) {
        return clickEventRepository.findTopReferrers(mappingId)
                .stream()
                .map(row -> AnalyticsResponse.ReferrerStat.builder()
                        .referer(String.valueOf(row[0]))
                        .clicks(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, Long> buildBrowserStats(Long mappingId) {
        Map<String, Long> result = new HashMap<>();
        clickEventRepository.findBrowserBreakdown(mappingId)
                .forEach(row -> result.put(
                        String.valueOf(row[0]),
                        ((Number) row[1]).longValue()
                ));
        return result;
    }
}
