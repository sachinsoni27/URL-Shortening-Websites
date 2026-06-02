package com.example.urlshortener.service;

import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.exception.AliasAlreadyTakenException;
import com.example.urlshortener.exception.UrlExpiredException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlMappingRepository;
import com.example.urlshortener.util.Base62Encoder;
import com.example.urlshortener.util.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UrlShortenerService}.
 *
 * Uses Mockito to isolate the service from JPA and Redis dependencies.
 */
@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock private UrlMappingRepository urlMappingRepository;
    @Mock private Base62Encoder base62Encoder;
    @Mock private UrlValidator urlValidator;

    @InjectMocks
    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
    }

    // ─── Shorten — auto-generated code ──────────────────────────────────────

    @Test
    @DisplayName("shorten with no alias generates a Base62 code from the saved ID")
    void shorten_noAlias_generatesCode() {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://example.com/very-long-url");

        UrlMapping savedWithTemp = UrlMapping.builder()
                .id(42L).code("TEMP").longUrl(request.getLongUrl()).clicks(0L)
                .createdAt(Instant.now()).build();

        when(urlMappingRepository.save(any())).thenReturn(savedWithTemp);
        when(base62Encoder.encode(42L)).thenReturn("0000a0");
        when(urlMappingRepository.existsByCode("0000a0")).thenReturn(false);
        when(urlMappingRepository.save(any())).thenReturn(
                savedWithTemp.toBuilder().code("0000a0").build());

        ShortenResponse response = service.shorten(request);

        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/0000a0");
        assertThat(response.getExpiresAt()).isNull();
        verify(urlValidator).validateUrl("https://example.com/very-long-url");
    }

    @Test
    @DisplayName("shorten with TTL sets expiresAt to approximately now + ttlDays")
    void shorten_withTtl_setsExpiry() {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://example.com");
        request.setTtlDays(30);

        Instant futureExpiry = Instant.now().plus(30, ChronoUnit.DAYS);

        UrlMapping saved = UrlMapping.builder()
                .id(1L).code("TEMP").longUrl(request.getLongUrl())
                .expiresAt(futureExpiry).clicks(0L).createdAt(Instant.now()).build();

        when(urlMappingRepository.save(any())).thenReturn(saved);
        when(base62Encoder.encode(1L)).thenReturn("000001");
        when(urlMappingRepository.existsByCode("000001")).thenReturn(false);
        when(urlMappingRepository.save(any())).thenReturn(saved.toBuilder().code("000001").build());

        ShortenResponse response = service.shorten(request);

        assertThat(response.getExpiresAt()).isNotNull();
        assertThat(response.getExpiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
    }

    // ─── Shorten — custom alias ──────────────────────────────────────────────

    @Test
    @DisplayName("shorten with unique custom alias saves alias as code")
    void shorten_customAlias_usesAliasAsCode() {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://example.com");
        request.setCustomAlias("my-link");

        when(urlMappingRepository.existsByCustomAlias("my-link")).thenReturn(false);
        when(urlMappingRepository.existsByCode("my-link")).thenReturn(false);
        when(urlMappingRepository.save(any())).thenReturn(
                UrlMapping.builder()
                        .id(5L).code("my-link").longUrl(request.getLongUrl())
                        .customAlias("my-link").clicks(0L).createdAt(Instant.now()).build());

        ShortenResponse response = service.shorten(request);

        assertThat(response.getCode()).isEqualTo("my-link");
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/my-link");
    }

    @Test
    @DisplayName("shorten with duplicate alias throws AliasAlreadyTakenException")
    void shorten_duplicateAlias_throwsException() {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://example.com");
        request.setCustomAlias("taken-alias");

        when(urlMappingRepository.existsByCustomAlias("taken-alias")).thenReturn(true);

        assertThatThrownBy(() -> service.shorten(request))
                .isInstanceOf(AliasAlreadyTakenException.class)
                .hasMessageContaining("taken-alias");
    }

    // ─── Resolve ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resolve finds an active URL mapping by code")
    void resolve_activeCode_returnsMapping() {
        UrlMapping mapping = UrlMapping.builder()
                .id(1L).code("abc123").longUrl("https://example.com")
                .clicks(5L).createdAt(Instant.now()).build();

        when(urlMappingRepository.findByCodeOrCustomAlias("abc123"))
                .thenReturn(Optional.of(mapping));

        UrlMapping result = service.resolve("abc123");

        assertThat(result.getLongUrl()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("resolve throws UrlNotFoundException for unknown code")
    void resolve_unknownCode_throwsNotFoundException() {
        when(urlMappingRepository.findByCodeOrCustomAlias("unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("unknown"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("resolve throws UrlExpiredException for expired mapping")
    void resolve_expiredCode_throwsExpiredException() {
        UrlMapping expired = UrlMapping.builder()
                .id(1L).code("oldcode")
                .longUrl("https://example.com")
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .clicks(0L).createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .build();

        when(urlMappingRepository.findByCodeOrCustomAlias("oldcode"))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolve("oldcode"))
                .isInstanceOf(UrlExpiredException.class)
                .hasMessageContaining("oldcode");
    }

    // ─── Delete ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteById removes the mapping when it exists")
    void deleteById_existingId_deletesSuccessfully() {
        when(urlMappingRepository.existsById(10L)).thenReturn(true);
        doNothing().when(urlMappingRepository).deleteById(10L);

        assertThatCode(() -> service.deleteById(10L)).doesNotThrowAnyException();

        verify(urlMappingRepository).deleteById(10L);
    }

    @Test
    @DisplayName("deleteById throws UrlNotFoundException for unknown ID")
    void deleteById_unknownId_throwsNotFoundException() {
        when(urlMappingRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteById(999L))
                .isInstanceOf(UrlNotFoundException.class);
    }
}
