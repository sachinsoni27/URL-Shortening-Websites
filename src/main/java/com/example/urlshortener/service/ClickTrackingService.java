package com.example.urlshortener.service;

import com.example.urlshortener.model.ClickEvent;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.ClickEventRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Asynchronous click event recording service.
 *
 * Recording happens in a background thread (@Async) so that the
 * redirect response is not blocked by the database write.
 *
 * IP address extraction honours the X-Forwarded-For header set by
 * reverse proxies / load balancers.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClickTrackingService {

    private final ClickEventRepository clickEventRepository;

    /**
     * Record a single click event asynchronously.
     *
     * @param mapping the URL mapping that was accessed
     * @param request the HTTP request (for headers / IP)
     */
    @Async
    @Transactional
    public void recordClick(UrlMapping mapping, HttpServletRequest request) {
        try {
            ClickEvent event = ClickEvent.builder()
                    .urlMapping(mapping)
                    .timestamp(Instant.now())
                    .ipAddress(extractIpAddress(request))
                    .userAgent(truncate(request.getHeader("User-Agent"), 512))
                    .referer(truncate(request.getHeader("Referer"), 2048))
                    // Country lookup can be wired here via a GeoIP2 bean
                    .country(null)
                    .build();

            clickEventRepository.save(event);
        } catch (Exception ex) {
            // Analytics failures must never affect redirect availability
            log.warn("Failed to record click for mapping id={}: {}",
                    mapping.getId(), ex.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Extract the real client IP, honouring X-Forwarded-For from proxies.
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // May be a comma-separated chain; take the first (original client)
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /** Safely truncate a string to a maximum length, returning null if input is null. */
    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
