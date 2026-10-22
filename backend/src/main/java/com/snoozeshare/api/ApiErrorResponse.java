package com.snoozeshare.api;

/** Stable, user-safe shape for API errors. */
public record ApiErrorResponse(String code, String message, String requestId) {
}
