package com.example.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a short URL is accessed after its TTL has elapsed.
 */
@ResponseStatus(HttpStatus.GONE)
public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String code) {
        super("The shortened URL with code '" + code + "' has expired");
    }
}
