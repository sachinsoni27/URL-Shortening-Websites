package com.example.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * URL Shortener Application — Entry Point
 *
 * Features:
 *  - URL shortening with Base62-encoded unique codes
 *  - Custom aliases with collision detection
 *  - TTL-based link expiration
 *  - Click analytics tracking (async)
 *  - Redis caching for hot URL lookups
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
