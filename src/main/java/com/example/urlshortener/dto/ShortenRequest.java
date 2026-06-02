package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

/**
 * Request body for POST /api/shorten
 */
@Data
public class ShortenRequest {

    /**
     * The original long URL to shorten. Must be a valid HTTP/HTTPS URL.
     */
    @NotBlank(message = "Long URL must not be blank")
    @URL(message = "Must be a valid URL (http or https)")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String longUrl;

    /**
     * Optional custom alias. URL-safe characters only (a-z, A-Z, 0-9, hyphens).
     * Between 3–50 characters if provided.
     */
    @Pattern(
        regexp = "^[a-zA-Z0-9\\-_]*$",
        message = "Alias may only contain letters, digits, hyphens, and underscores"
    )
    @Size(min = 3, max = 50, message = "Alias must be between 3 and 50 characters")
    private String customAlias;

    /**
     * Optional TTL in days. Null or 0 means the link is permanent.
     * Maximum is 3650 days (10 years).
     */
    @Positive(message = "TTL must be a positive number of days")
    private Integer ttlDays;
}
