package com.example.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Generic API response wrapper for all REST endpoints.
 *
 * Success: { "success": true,  "data": {...}, "error": null }
 * Failure: { "success": false, "data": null,  "error": "message" }
 *
 * @param <T> The payload type for the data field
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String error;

    private ApiResponse(boolean success, T data, String error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    /** Build a successful response with a data payload. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /** Build an error response with a descriptive message. */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
