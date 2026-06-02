package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link UrlMapping} entities.
 *
 * The `code` column has a unique index in Oracle — findByCode() maps to an
 * index range scan, giving O(log n) ≈ O(1) lookup at table scale.
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Primary lookup for redirect resolution.
     * Hits idx_url_mappings_code unique index.
     */
    Optional<UrlMapping> findByCode(String code);

    /**
     * Lookup by user-supplied custom alias.
     * Hits idx_url_mappings_alias unique index.
     */
    Optional<UrlMapping> findByCustomAlias(String customAlias);

    /** Existence check — used during code generation to detect collisions. */
    boolean existsByCode(String code);

    /** Existence check — used when validating a new custom alias. */
    boolean existsByCustomAlias(String alias);

    /**
     * Paginated list of all URL mappings, newest first.
     * Used by GET /api/urls.
     */
    Page<UrlMapping> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Atomically increment the denormalized click counter.
     * Executed in the redirect hot-path — bypasses dirty-checking overhead.
     */
    @Modifying
    @Query("UPDATE UrlMapping u SET u.clicks = u.clicks + 1 WHERE u.id = :id")
    void incrementClicks(@Param("id") Long id);

    /**
     * Resolve a short code OR a custom alias in a single query.
     * Useful when the redirect controller doesn't know which is which.
     */
    @Query("SELECT u FROM UrlMapping u WHERE u.code = :token OR u.customAlias = :token")
    Optional<UrlMapping> findByCodeOrCustomAlias(@Param("token") String token);
}
