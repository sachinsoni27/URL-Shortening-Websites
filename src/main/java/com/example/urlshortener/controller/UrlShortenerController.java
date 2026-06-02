package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ApiResponse;
import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.dto.UrlSummary;
import com.example.urlshortener.service.UrlShortenerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for URL shortening CRUD operations.
 *
 * Endpoints:
 *   POST   /api/shorten     — create a shortened URL
 *   GET    /api/urls        — list all URLs (paginated)
 *   DELETE /api/urls/{id}   — delete a URL by ID
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;

    /**
     * POST /api/shorten
     *
     * Creates a new shortened URL entry. Returns 201 Created with the full
     * short URL, code, creation timestamp, and optional expiry.
     */
    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<ShortenResponse>> shorten(
            @Valid @RequestBody ShortenRequest request) {

        log.info("Shorten request for: {}", request.getLongUrl());
        ShortenResponse response = urlShortenerService.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    /**
     * GET /api/urls?page=0&size=20
     *
     * Returns a paginated list of all shortened URLs, newest first.
     */
    @GetMapping("/urls")
    public ResponseEntity<ApiResponse<Page<UrlSummary>>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<UrlSummary> urls = urlShortenerService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(urls));
    }

    /**
     * DELETE /api/urls/{id}
     *
     * Permanently deletes a URL mapping and evicts its Redis cache entry.
     * Returns 204 No Content on success.
     */
    @DeleteMapping("/urls/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        urlShortenerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
