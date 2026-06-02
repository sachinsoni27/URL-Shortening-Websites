package com.example.urlshortener.repository;

import com.example.urlshortener.model.ClickEvent;
import com.example.urlshortener.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ClickEvent} analytics data.
 *
 * All aggregate queries leverage the composite index on (url_mapping_id, timestamp)
 * to avoid full-table scans on high-volume click data.
 */
@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    /** Total number of clicks for a given mapping (fast index scan). */
    long countByUrlMapping(UrlMapping urlMapping);

    /** Number of distinct IP addresses — approximates unique visitors. */
    @Query("SELECT COUNT(DISTINCT c.ipAddress) FROM ClickEvent c WHERE c.urlMapping = :mapping")
    long countDistinctIpByUrlMapping(@Param("mapping") UrlMapping mapping);

    /**
     * Daily click breakdown — returns rows of [date_string, count].
     *
     * Uses Oracle TRUNC(timestamp, 'DD') to group by day.
     * The composite index is used for the WHERE clause filter on url_mapping_id.
     */
    @Query(value = """
        SELECT TO_CHAR(TRUNC(c.timestamp, 'DD'), 'YYYY-MM-DD') AS click_date,
               COUNT(*) AS click_count
        FROM click_events c
        WHERE c.url_mapping_id = :mappingId
        GROUP BY TRUNC(c.timestamp, 'DD')
        ORDER BY TRUNC(c.timestamp, 'DD') ASC
        """, nativeQuery = true)
    List<Object[]> findDailyClickCounts(@Param("mappingId") Long mappingId);

    /**
     * Top 10 referrers by click count for a given mapping.
     * NULL referers are grouped under 'Direct'.
     */
    @Query(value = """
        SELECT NVL(c.referer, 'Direct') AS referer,
               COUNT(*) AS click_count
        FROM click_events c
        WHERE c.url_mapping_id = :mappingId
        GROUP BY NVL(c.referer, 'Direct')
        ORDER BY click_count DESC
        FETCH FIRST 10 ROWS ONLY
        """, nativeQuery = true)
    List<Object[]> findTopReferrers(@Param("mappingId") Long mappingId);

    /**
     * Browser/device breakdown parsed from User-Agent prefix patterns.
     *
     * Returns rows of [browser_label, count]. Simplified pattern matching
     * that covers the most common browsers.
     */
    @Query(value = """
        SELECT
            CASE
                WHEN c.user_agent LIKE '%Edg/%'    THEN 'Edge'
                WHEN c.user_agent LIKE '%Chrome/%'  THEN 'Chrome'
                WHEN c.user_agent LIKE '%Firefox/%' THEN 'Firefox'
                WHEN c.user_agent LIKE '%Safari/%'  THEN 'Safari'
                WHEN c.user_agent LIKE '%OPR/%'     THEN 'Opera'
                WHEN c.user_agent LIKE '%bot%'      THEN 'Bot/Crawler'
                WHEN c.user_agent IS NULL           THEN 'Unknown'
                ELSE 'Other'
            END AS browser,
            COUNT(*) AS click_count
        FROM click_events c
        WHERE c.url_mapping_id = :mappingId
        GROUP BY
            CASE
                WHEN c.user_agent LIKE '%Edg/%'    THEN 'Edge'
                WHEN c.user_agent LIKE '%Chrome/%'  THEN 'Chrome'
                WHEN c.user_agent LIKE '%Firefox/%' THEN 'Firefox'
                WHEN c.user_agent LIKE '%Safari/%'  THEN 'Safari'
                WHEN c.user_agent LIKE '%OPR/%'     THEN 'Opera'
                WHEN c.user_agent LIKE '%bot%'      THEN 'Bot/Crawler'
                WHEN c.user_agent IS NULL           THEN 'Unknown'
                ELSE 'Other'
            END
        ORDER BY click_count DESC
        """, nativeQuery = true)
    List<Object[]> findBrowserBreakdown(@Param("mappingId") Long mappingId);
}
