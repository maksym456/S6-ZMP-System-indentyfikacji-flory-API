package com.ezielnik.api.config;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();

        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = "Unexpected error";
        }

        return ResponseEntity
                .status(statusCode)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message);
    }
}