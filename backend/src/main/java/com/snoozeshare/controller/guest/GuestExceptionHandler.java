package com.snoozeshare.controller.guest;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.snoozeshare.config.ConditionalOnDatabase;

import jakarta.validation.ConstraintViolationException;

/**
 * Maps service-layer exceptions to structured JSON error responses
 * for all controllers in the guest package.
 */
@RestControllerAdvice(basePackages = "com.snoozeshare.controller.guest")
@ConditionalOnDatabase
public class GuestExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex) {
        return error(403, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        int status = 400;
        String code = "BAD_REQUEST";
        if (msg.contains("does not exist") || msg.contains("not found")) {
            status = 404;
            code = "LISTING_NOT_FOUND";
        } else if (msg.contains("is not active")) {
            code = "LISTING_INACTIVE";
        } else if (msg.contains("not available")) {
            status = 409;
            code = "DATES_UNAVAILABLE";
        } else if (msg.contains("Cannot book your own")) {
            code = "SELF_BOOKING";
        } else if (msg.contains("Start and end dates")) {
            code = "VALIDATION_ERROR";
        } else if (msg.contains("End date must be")) {
            code = "INVALID_DATE_RANGE";
        } else if (msg.contains("Insufficient")
                || msg.contains("insufficient")) {
            code = "INSUFFICIENT_BALANCE";
        }
        return error(status, code, msg);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        int status = 400;
        String code = "BAD_REQUEST";
        if (msg.contains("Cannot cancel")) {
            code = "NOT_CANCELLABLE";
        } else if (msg.contains("not eligible for a dispute")) {
            code = "DISPUTE_WINDOW_CLOSED";
        } else if (msg.contains("already exists for this booking")
                && msg.contains("ticket")) {
            status = 409;
            code = "DUPLICATE_TICKET";
        } else if (msg.contains("already exists for this booking")
                && msg.contains("review")) {
            status = 409;
            code = "DUPLICATE_REVIEW";
        } else if (msg.contains("Ticket is resolved")
                || msg.contains("Chat is closed")) {
            code = "THREAD_CLOSED";
        } else if (msg.contains("only be submitted for completed")) {
            code = "NOT_REVIEWABLE";
        } else if (msg.contains("Insufficient")
                || msg.contains("insufficient")) {
            code = "INSUFFICIENT_BALANCE";
        }
        return error(status, code, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException ex) {
        return error(400, "VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return error(400, "VALIDATION_ERROR", msg);
    }

    private ResponseEntity<Map<String, String>> error(
            int status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "code", code,
                "message", message,
                "requestId", UUID.randomUUID().toString()));
    }
}
