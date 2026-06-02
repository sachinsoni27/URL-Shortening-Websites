package com.example.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user attempts to register a custom alias that already exists.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class AliasAlreadyTakenException extends RuntimeException {

    public AliasAlreadyTakenException(String alias) {
        super("The alias '" + alias + "' is already taken. Please choose another.");
    }
}
