package com.events.goup.exception;

import java.time.LocalDateTime;

import java.util.List;

public record ApiError(LocalDateTime timestamp, int status, String message, List<FieldError> errors) {

    public ApiError(LocalDateTime timestamp, int status, String message) {
        this(timestamp, status, message, null);
    }
}