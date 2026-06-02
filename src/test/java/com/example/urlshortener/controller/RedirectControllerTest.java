package com.example.urlshortener.controller;

import com.example.urlshortener.exception.GlobalExceptionHandler;
import com.example.urlshortener.exception.UrlExpiredException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.service.ClickTrackingService;
import com.example.urlshortener.service.UrlShortenerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link RedirectController}.
 *
 * Verifies redirect behaviour for valid, expired, and unknown short codes.
 */
@WebMvcTest(RedirectController.class)
@Import(GlobalExceptionHandler.class)
class RedirectControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private UrlShortenerService urlShortenerService;
    @MockBean private ClickTrackingService clickTrackingService;

    @Test
    @DisplayName("GET /{code} with valid code returns HTTP 302 redirect")
    void redirect_validCode_returns302() throws Exception {
        UrlMapping mapping = UrlMapping.builder()
                .id(1L)
                .code("abc123")
                .longUrl("https://example.com/target-page")
                .clicks(0L)
                .createdAt(Instant.now())
                .build();

        when(urlShortenerService.resolve("abc123")).thenReturn(mapping);
        doNothing().when(clickTrackingService).recordClick(any(), any(), any(), any());
        doNothing().when(urlShortenerService).incrementClicks(1L);

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target-page"));

        verify(urlShortenerService).resolve("abc123");
        verify(urlShortenerService).incrementClicks(1L);
    }

    @Test
    @DisplayName("GET /{code} with unknown code returns 404")
    void redirect_unknownCode_returns404() throws Exception {
        when(urlShortenerService.resolve("unknown"))
                .thenThrow(new UrlNotFoundException("unknown"));

        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /{code} with expired code returns 410 Gone")
    void redirect_expiredCode_returns410() throws Exception {
        when(urlShortenerService.resolve("expired"))
                .thenThrow(new UrlExpiredException("expired"));

        mockMvc.perform(get("/expired"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Click tracking service is called asynchronously after redirect")
    void redirect_callsClickTracker() throws Exception {
        UrlMapping mapping = UrlMapping.builder()
                .id(2L).code("track01").longUrl("https://target.com")
                .clicks(10L).createdAt(Instant.now()).build();

        when(urlShortenerService.resolve("track01")).thenReturn(mapping);
        doNothing().when(clickTrackingService).recordClick(any(), any(), any(), any());
        doNothing().when(urlShortenerService).incrementClicks(2L);

        mockMvc.perform(get("/track01")).andExpect(status().isFound());

        // Verify click tracking was triggered
        verify(clickTrackingService).recordClick(eq(mapping), any(), any(), any());
        verify(urlShortenerService).incrementClicks(2L);
    }
}
