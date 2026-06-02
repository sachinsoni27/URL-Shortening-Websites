package com.example.urlshortener.controller;

import com.example.urlshortener.dto.ApiResponse;
import com.example.urlshortener.dto.ShortenRequest;
import com.example.urlshortener.dto.ShortenResponse;
import com.example.urlshortener.dto.UrlSummary;
import com.example.urlshortener.exception.GlobalExceptionHandler;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.service.UrlShortenerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link UrlShortenerController} using @WebMvcTest.
 *
 * No real Spring context or database required. All service calls are mocked.
 */
@WebMvcTest(UrlShortenerController.class)
@Import(GlobalExceptionHandler.class)
class UrlShortenerControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UrlShortenerService urlShortenerService;

    // ─── POST /api/shorten ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/shorten returns 201 with short URL on valid request")
    void shorten_validRequest_returns201() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://example.com/some-very-long-url");

        ShortenResponse response = ShortenResponse.builder()
                .shortUrl("http://localhost:8080/abc123")
                .code("abc123")
                .longUrl(request.getLongUrl())
                .createdAt(Instant.now())
                .clicks(0L)
                .build();

        when(urlShortenerService.shorten(any(ShortenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("abc123"))
                .andExpect(jsonPath("$.data.shortUrl").value("http://localhost:8080/abc123"));
    }

    @Test
    @DisplayName("POST /api/shorten returns 400 when URL is blank")
    void shorten_blankUrl_returns400() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("");  // blank

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/shorten returns 400 when URL has invalid scheme")
    void shorten_ftpUrl_returns400() throws Exception {
        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("ftp://example.com/file");

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/urls ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/urls returns 200 with paginated list")
    void listAll_returnsPagedResults() throws Exception {
        UrlSummary summary = UrlSummary.builder()
                .id(1L).code("abc123").shortUrl("http://localhost:8080/abc123")
                .longUrl("https://example.com").clicks(42L)
                .createdAt(Instant.now()).build();

        var page = new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1);
        when(urlShortenerService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].code").value("abc123"))
                .andExpect(jsonPath("$.data.content[0].clicks").value(42));
    }

    // ─── DELETE /api/urls/{id} ──────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/urls/{id} returns 204 on success")
    void delete_existingId_returns204() throws Exception {
        doNothing().when(urlShortenerService).deleteById(1L);

        mockMvc.perform(delete("/api/urls/1"))
                .andExpect(status().isNoContent());

        verify(urlShortenerService).deleteById(1L);
    }

    @Test
    @DisplayName("DELETE /api/urls/{id} returns 404 when ID does not exist")
    void delete_unknownId_returns404() throws Exception {
        doThrow(new UrlNotFoundException("id=999"))
                .when(urlShortenerService).deleteById(999L);

        mockMvc.perform(delete("/api/urls/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
