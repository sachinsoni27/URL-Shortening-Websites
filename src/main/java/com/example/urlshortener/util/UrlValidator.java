package com.example.urlshortener.util;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * URL and alias validation utilities.
 */
@Component
public class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_ALIAS_LENGTH = 50;
    private static final int MIN_ALIAS_LENGTH = 3;

    /**
     * Reserved short codes / aliases that the system uses internally.
     * Users may not register these as custom aliases.
     */
    private static final Set<String> RESERVED_PATHS = Set.of(
        "api", "static", "css", "js", "img", "favicon.ico",
        "actuator", "health", "metrics", "index", "404", "error"
    );

    /**
     * Validate a URL:
     *  - Must not be blank or exceed 2048 chars
     *  - Must have a valid scheme (http or https)
     *  - Must have a parseable host
     *
     * @param url the URL string to validate
     * @throws IllegalArgumentException with a descriptive message on failure
     */
    public void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        if (url.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException(
                "URL exceeds maximum length of " + MAX_URL_LENGTH + " characters");
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException(
                "URL scheme must be one of: " + ALLOWED_SCHEMES + ". Got: " + scheme);
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("URL must have a valid host");
        }
    }

    /**
     * Validate a custom alias:
     *  - Length between 3 and 50 characters
     *  - Only URL-safe characters: letters, digits, hyphens, underscores
     *  - Not a reserved system path
     *
     * @param alias the alias to validate
     * @throws IllegalArgumentException with a descriptive message on failure
     */
    public void validateAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return; // null alias is acceptable (auto-generate)
        }
        if (alias.length() < MIN_ALIAS_LENGTH) {
            throw new IllegalArgumentException(
                "Alias must be at least " + MIN_ALIAS_LENGTH + " characters long");
        }
        if (alias.length() > MAX_ALIAS_LENGTH) {
            throw new IllegalArgumentException(
                "Alias must not exceed " + MAX_ALIAS_LENGTH + " characters");
        }
        if (!alias.matches("^[a-zA-Z0-9\\-_]+$")) {
            throw new IllegalArgumentException(
                "Alias may only contain letters, digits, hyphens, and underscores");
        }
        if (RESERVED_PATHS.contains(alias.toLowerCase())) {
            throw new IllegalArgumentException(
                "'" + alias + "' is a reserved path and cannot be used as an alias");
        }
    }
}
