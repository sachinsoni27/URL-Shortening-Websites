package com.example.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.urlshortener.model.UrlMapping;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Lightweight summary of a UrlMapping used in the paginated URL list.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UrlSummary {

    private Long id;
    private String code;
    private String shortUrl;
    private String longUrl;
    private String customAlias;
    private Long clicks;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean expired;

    /** Map a UrlMapping entity to this DTO, injecting the base URL for shortUrl. */
    public static UrlSummary from(UrlMapping mapping, String baseUrl) {
        String effectiveCode = mapping.getCustomAlias() != null
                ? mapping.getCustomAlias()
                : mapping.getCode();

        return UrlSummary.builder()
                .id(mapping.getId())
                .code(mapping.getCode())
                .shortUrl(baseUrl + "/" + effectiveCode)
                .longUrl(mapping.getLongUrl())
                .customAlias(mapping.getCustomAlias())
                .clicks(mapping.getClicks())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .expired(mapping.isExpired())
                .build();
    }
}
