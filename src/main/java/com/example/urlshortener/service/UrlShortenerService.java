package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.dto.UrlSummary;
import com.example.urlshortener.exception.AliasAlreadyTakenException;
import com.example.urlshortener.exception.UrlExpiredException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.Base62Encoder;
import com.example.urlshortener.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Core URL shortening service.
 *
 * Code generation strategy:
 *   1. Persist a new UrlMapping with a temporary placeholder code.
 *   2. Use the auto-generated DB ID to produce a collision-resistant Base62 code.
 *   3. Update the code field — guaranteed unique because ID is globally unique.
 *
 * Redis caching:
 *   - Cache key "urls::{token}" stores the resolved UrlMapping.
 *   - Evicted on deletion to prevent stale cache hits.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;
    private final Base62Encoder base62Encoder;
    private final UrlValidator urlValidator;

    @Value("${server.base-url}")
    private String baseUrl;

    // ─── Create ─────────────────────────────────────────────────────────────

    /**
     * Shorten a URL. Handles both auto-generated and custom alias modes.
     *
     * @param request the shorten request with URL, optional alias, and optional TTL
     * @return the shortened URL response
     */
    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        urlValidator.validateUrl(request.getLongUrl());

        String alias = request.getCustomAlias();
        if (alias != null && !alias.isBlank()) {
            return shortenWithCustomAlias(request, alias.trim());
        } else {
            return shortenWithGeneratedCode(request);
        }
    }

    private ShortenResponse shortenWithCustomAlias(ShortenRequest request, String alias) {
        urlValidator.validateAlias(alias);

        if (urlMappingRepository.existsByCustomAlias(alias)) {
            throw new AliasAlreadyTakenException(alias);
        }

        // Also ensure the alias doesn't clash with an existing auto-generated code
        if (urlMappingRepository.existsByCode(alias)) {
            throw new AliasAlreadyTakenException(alias);
        }

        Instant expiresAt = computeExpiry(request.getTtlDays());
        UrlMapping mapping = UrlMapping.builder()
                .code(alias)            // use alias as the code too for simplicity
                .longUrl(request.getLongUrl())
                .customAlias(alias)
                .expiresAt(expiresAt)
                .clicks(0L)
                .build();

        mapping = urlMappingRepository.save(mapping);
        log.info("Created custom alias '{}' → {}", alias, request.getLongUrl());
        return buildResponse(mapping, alias);
    }

    private ShortenResponse shortenWithGeneratedCode(ShortenRequest request) {
        Instant expiresAt = computeExpiry(request.getTtlDays());

        // Save first to get the DB-generated ID
        UrlMapping mapping = UrlMapping.builder()
                .code("TEMP")           // temporary placeholder
                .longUrl(request.getLongUrl())
                .expiresAt(expiresAt)
                .clicks(0L)
                .build();

        mapping = urlMappingRepository.save(mapping);

        // Encode the ID to produce a unique code
        String code = base62Encoder.encode(mapping.getId());

        // Handle the extremely unlikely edge case of a collision
        int attempts = 0;
        while (urlMappingRepository.existsByCode(code) && attempts < 5) {
            code = base62Encoder.encode(mapping.getId() + (long) Math.pow(10, attempts + 6));
            attempts++;
        }

        mapping.setCode(code);
        mapping = urlMappingRepository.save(mapping);

        log.info("Created short code '{}' → {}", code, request.getLongUrl());
        return buildResponse(mapping, code);
    }

    // ─── Resolve ─────────────────────────────────────────────────────────────

    /**
     * Look up the UrlMapping for a given short token (code or alias).
     * Result is cached in Redis; expired URLs are evicted and return a 410.
     *
     * @param token the short code or custom alias from the URL path
     * @return the UrlMapping entity
     */
    @Cacheable(value = "urls", key = "#token")
    @Transactional(readOnly = true)
    public UrlMapping resolve(String token) {
        UrlMapping mapping = urlMappingRepository.findByCodeOrCustomAlias(token)
                .orElseThrow(() -> new UrlNotFoundException(token));

        if (mapping.isExpired()) {
            throw new UrlExpiredException(token);
        }

        return mapping;
    }

    /**
     * Increment the denormalized click counter without loading the full entity.
     * Called separately from resolve() to keep the cacheable path clean.
     */
    @Transactional
    public void incrementClicks(Long mappingId) {
        urlMappingRepository.incrementClicks(mappingId);
    }

    // ─── List ─────────────────────────────────────────────────────────────────

    /**
     * Return a paginated list of all URL mappings, newest first.
     */
    @Transactional(readOnly = true)
    public Page<UrlSummary> getAll(Pageable pageable) {
        return urlMappingRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(m -> UrlSummary.from(m, baseUrl));
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    /**
     * Delete a URL mapping by ID. Evicts the Redis cache entry.
     */
    @Transactional
    @CacheEvict(value = "urls", allEntries = true)
    public void deleteById(Long id) {
        if (!urlMappingRepository.existsById(id)) {
            throw new UrlNotFoundException("id=" + id);
        }
        urlMappingRepository.deleteById(id);
        log.info("Deleted URL mapping id={}", id);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Instant computeExpiry(Integer ttlDays) {
        if (ttlDays == null || ttlDays <= 0) return null;
        return Instant.now().plus(ttlDays, ChronoUnit.DAYS);
    }

    private ShortenResponse buildResponse(UrlMapping mapping, String effectiveCode) {
        return ShortenResponse.builder()
                .shortUrl(baseUrl + "/" + effectiveCode)
                .code(effectiveCode)
                .longUrl(mapping.getLongUrl())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .clicks(mapping.getClicks())
                .build();
    }
}
