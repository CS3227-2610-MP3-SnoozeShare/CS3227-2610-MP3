package com.snoozeshare.auth.api;

/** Stable, safe error response used by auth endpoints. */
public record AuthErrorResponse(String code, String message, String requestId) { }
