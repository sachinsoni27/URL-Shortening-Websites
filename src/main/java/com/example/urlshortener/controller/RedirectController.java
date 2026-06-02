package com.example.urlshortener.controller;

import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.service.ClickTrackingService;
import com.example.urlshortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Handles the core redirect operation: GET /{code}
 *
 * Flow:
 *   1. Resolve the short code / alias (cached via Redis)
 *   2. Fire-and-forget async click event recording
 *   3. Increment denormalized click counter
 *   4. Return HTTP 302 redirect to the original URL
 *
 * Uses 302 (Found) so browsers re-check the destination on every visit,
 * enabling click tracking even from cached browser history.
 * Change to 301 for pure SEO use-cases where caching is acceptable.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RedirectController {

    private final UrlShortenerService urlShortenerService;
    private final ClickTrackingService clickTrackingService;

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(
            @PathVariable String code,
            HttpServletRequest request) {

        log.debug("Redirect request for code: {}", code);

        // Resolve from cache or DB (throws UrlNotFoundException / UrlExpiredException)
        UrlMapping mapping = urlShortenerService.resolve(code);

        // Async: record click without blocking the response
        clickTrackingService.recordClick(mapping, request);

        // Async-safe: increment the denormalized counter via JPQL UPDATE
        urlShortenerService.incrementClicks(mapping.getId());

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(mapping.getLongUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
