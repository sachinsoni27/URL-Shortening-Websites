package com.example.urlshortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Records each individual redirect event for analytics purposes.
 *
 * Indexes:
 *  - (url_mapping_id, timestamp) composite — for range-based daily analytics queries
 *  - ip_address — for unique-visitor aggregation
 */
@Entity
@Table(
    name = "click_events",
    indexes = {
        @Index(name = "idx_click_events_mapping_ts", columnList = "url_mapping_id, timestamp"),
        @Index(name = "idx_click_events_ip", columnList = "ip_address")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "click_event_seq_gen")
    @SequenceGenerator(
        name = "click_event_seq_gen",
        sequenceName = "click_event_seq",
        allocationSize = 50    // Batch-friendly allocation
    )
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * The URL mapping this click belongs to.
     * LAZY fetch to avoid N+1 queries when loading click lists.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_mapping_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_click_events_url_mapping"))
    private UrlMapping urlMapping;

    /**
     * When the click occurred.
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * Client IP address (may be IPv4 or IPv6).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Full User-Agent header string from the browser.
     */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    /**
     * HTTP Referer header — source page of the request.
     */
    @Column(name = "referer", length = 2048)
    private String referer;

    /**
     * Country code derived from GeoIP lookup (e.g., "IN", "US").
     * Nullable — populated only when GeoIP is configured.
     */
    @Column(name = "country", length = 10)
    private String country;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
