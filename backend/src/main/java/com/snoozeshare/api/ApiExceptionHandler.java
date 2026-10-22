package com.snoozeshare.api;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.snoozeshare.auth.security.ProtectedRequestPath;
import com.snoozeshare.service.NotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/** Maps application and framework failures to a safe, consistent API response. */
@org.springframework.web.bind.annotation.RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String INTERNAL_MESSAGE = "Something went wrong";
    private static final String VALIDATION_MESSAGE = "Request is not valid";

    @org.springframework.web.bind.annotation.ExceptionHandler(MissingActorException.class)
    public ResponseEntity<Object> handleMissingActor(MissingActorException exception,
                                                      WebRequest request) {
        return respond(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                "Request cannot be authorized", exception, request, new HttpHeaders());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException exception,
                                                 WebRequest request) {
        String message = ownMessage(exception) ? exception.getMessage() : "Not found";
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", message,
                exception, request, new HttpHeaders());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException exception,
                                                        WebRequest request) {
        if (ownMessage(exception)) {
            return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(),
                    exception, request, new HttpHeaders());
        }
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", VALIDATION_MESSAGE,
                exception, request, new HttpHeaders());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalState(IllegalStateException exception,
                                                     WebRequest request) {
        if (ownMessage(exception)) {
            return respond(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(),
                    exception, request, new HttpHeaders());
        }
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", INTERNAL_MESSAGE,
                exception, request, new HttpHeaders());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<Object> handleLockFailure(
            PessimisticLockingFailureException exception, WebRequest request) {
        return respond(HttpStatus.CONFLICT, "CONFLICT",
                "Another change is in progress. Try again.", exception, request, new HttpHeaders());
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception exception, WebRequest request) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", INTERNAL_MESSAGE,
                exception, request, new HttpHeaders());
    }

    private ResponseEntity<Object> respond(HttpStatusCode status,
                                           String code,
                                           String message,
                                           Exception exception,
                                           WebRequest webRequest,
                                           HttpHeaders headers) {
        String requestId = UUID.randomUUID().toString();
        HttpServletRequest request = webRequest instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest() : null;
        String path = request == null ? "unknown" : ProtectedRequestPath.withinApplication(request);
        Object actor = request == null ? null : request.getAttribute(CurrentUserIdResolver.ATTRIBUTE);
        String actorId = actor instanceof UUID id ? id.toString() : "unknown";
        if (status.is5xxServerError()) {
            Exception sanitized = sanitized(exception);
            LOGGER.error("API request failed requestId={} status={} code={} path={} agentId={} exceptionType={}",
                    requestId, status.value(), code, path, actorId,
                    exception.getClass().getName(), sanitized);
        } else {
            LOGGER.warn("API request failed requestId={} status={} code={} path={} agentId={}",
                    requestId, status.value(), code, path, actorId);
        }
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new ApiErrorResponse(code, message, requestId), headers, status);
    }

    private static Exception sanitized(Exception exception) {
        Exception safe = new Exception(exception.getClass().getName());
        safe.setStackTrace(exception.getStackTrace());
        return safe;
    }

    private static boolean ownMessage(Throwable exception) {
        StackTraceElement[] stack = exception.getStackTrace();
        if (stack.length == 0) {
            return false;
        }
        String owner = stack[0].getClassName();
        return owner.startsWith("com.snoozeshare.service.")
                || owner.startsWith("com.snoozeshare.domain.");
    }

}
