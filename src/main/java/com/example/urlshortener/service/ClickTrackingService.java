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
     * @param mapping   the URL mapping that was accessed
     * @param ipAddress the client IP address
     * @param userAgent the client User-Agent header
     * @param referer   the client Referer header
     */
    @Async
    @Transactional
    public void recordClick(UrlMapping mapping, String ipAddress, String userAgent, String referer) {
        try {
            ClickEvent event = ClickEvent.builder()
                    .urlMapping(mapping)
                    .timestamp(Instant.now())
                    .ipAddress(ipAddress)
                    .userAgent(truncate(userAgent, 512))
                    .referer(truncate(referer, 2048))
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

    /** Safely truncate a string to a maximum length, returning null if input is null. */
    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
}
