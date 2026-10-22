package com.snoozeshare.api.error;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.snoozeshare.auth.api.AuthErrorResponse;
import com.snoozeshare.service.AvailabilityConflictException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/** Maps Host API failures to stable, safe responses without exposing exception details. */
@RestControllerAdvice(basePackages = "com.snoozeshare.api.controller")
public class HostApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostApiExceptionHandler.class);
    private static final String SAFE_MESSAGE = "Request could not be completed";

    @ExceptionHandler(HostApiException.class)
    public ResponseEntity<AuthErrorResponse> handleHostApiException(
        HostApiException exception,
        HttpServletRequest request) {
        return respond(exception.status(), exception.code(), request);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        ConstraintViolationException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<AuthErrorResponse> handleValidationException(
        Exception exception,
        HttpServletRequest request) {
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", request);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<AuthErrorResponse> handleMissingResource(
        EmptyResultDataAccessException exception,
        HttpServletRequest request) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthErrorResponse> handleDomainRule(
        IllegalArgumentException exception,
        HttpServletRequest request) {
        return respond(HttpStatus.UNPROCESSABLE_ENTITY, "DOMAIN_RULE_VIOLATION", request);
    }

    @ExceptionHandler(AvailabilityConflictException.class)
    public ResponseEntity<AuthErrorResponse> handleAvailabilityConflict(
        AvailabilityConflictException exception,
        HttpServletRequest request) {
        String code = exception.getConflictType() == AvailabilityConflictException.ConflictType.BOOKING
            ? "BOOKING_OVERLAP" : "HOST_BLOCK_OVERLAP";
        return respond(HttpStatus.CONFLICT, code, request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AuthErrorResponse> handleStaleState(
        IllegalStateException exception,
        HttpServletRequest request) {
        return respond(HttpStatus.CONFLICT, "STATE_CONFLICT", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthErrorResponse> handleUnexpected(
        Exception exception,
        HttpServletRequest request) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", request);
    }

    private ResponseEntity<AuthErrorResponse> respond(HttpStatus status, String code, HttpServletRequest request) {
        String requestId = UUID.randomUUID().toString();
        LOGGER.warn("Host API request failed requestId={} status={} code={} path={}",
            requestId, status.value(), code, request.getRequestURI());
        AuthErrorResponse body = new AuthErrorResponse(code, SAFE_MESSAGE, requestId);
        return ResponseEntity.status(status)
            .header("Cache-Control", "no-store")
            .body(body);
    }
}
