package com.example.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Analytics payload for GET /api/analytics/{code}
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsResponse {

    /** The short code being analysed */
    private String code;

    /** The original long URL */
    private String longUrl;

    /** Denormalized total click count (fast read from url_mappings) */
    private Long totalClicks;

    /** Number of unique visitors by IP address */
    private Long uniqueClicks;

    /**
     * Daily click breakdown: date string (yyyy-MM-dd) → click count
     * Sorted chronologically.
     */
    private List<DailyStat> dailyBreakdown;

    /**
     * Top referrers sorted by click count descending.
     */
    private List<ReferrerStat> topReferrers;

    /**
     * Browser/OS breakdown parsed from User-Agent strings.
     */
    private Map<String, Long> browserBreakdown;

    // ─── Nested stat types ──────────────────────────────────────────────────

    @Data
    @Builder
    public static class DailyStat {
        private String date;
        private Long clicks;
    }

    @Data
    @Builder
    public static class ReferrerStat {
        private String referer;
        private Long clicks;
    }
}
