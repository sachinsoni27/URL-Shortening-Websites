package com.example.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Response body for POST /api/shorten
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenResponse {

    /** The full shortened URL (e.g., http://localhost:8080/abc123) */
    private String shortUrl;

    /** The generated or custom alias code (e.g., abc123) */
    private String code;

    /** The original long URL that was shortened */
    private String longUrl;

    /** Timestamp when this mapping was created */
    private Instant createdAt;

    /**
     * Timestamp when this link expires.
     * Null if the link is permanent.
     */
    private Instant expiresAt;

    /** Current click count */
    private Long clicks;
}
