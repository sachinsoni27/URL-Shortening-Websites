package com.example.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a shortened URL mapping stored in Oracle SQL.
 *
 * Indexes:
 *  - code (unique) — primary lookup path for redirects
 *  - customAlias (unique, nullable) — user-supplied alias lookup
 */
@Entity
@Table(
    name = "url_mappings",
    indexes = {
        @Index(name = "idx_url_mappings_code", columnList = "code", unique = true),
        @Index(name = "idx_url_mappings_alias", columnList = "custom_alias", unique = true)
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "url_mapping_seq_gen")
    @SequenceGenerator(
        name = "url_mapping_seq_gen",
        sequenceName = "url_mapping_seq",
        allocationSize = 1
    )
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * The 6-8 character Base62-encoded short code.
     * Indexed for O(1) approximate lookup via index scan.
     */
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    /**
     * The original long URL this code redirects to.
     */
    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    /**
     * Optional user-supplied alias (e.g. "my-link").
     * Must be URL-safe and unique across all mappings.
     */
    @Column(name = "custom_alias", unique = true, length = 100)
    private String customAlias;

    /**
     * Creation timestamp. Set automatically on persist.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Expiration timestamp. Null means the link is permanent.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Denormalized click counter — incremented on each redirect.
     * Kept separate from ClickEvent count for fast reads.
     */
    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (clicks == null) {
            clicks = 0L;
        }
    }

    /**
     * Returns true if this URL has expired at the given time.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
