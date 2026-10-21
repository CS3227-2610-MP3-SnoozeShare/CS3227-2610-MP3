package com.snoozeshare.auth.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.auth.api.AuthErrorResponse;
import com.snoozeshare.auth.api.AuthProfileResponse;
import com.snoozeshare.auth.api.RegisterRequest;
import com.snoozeshare.auth.api.RegistrationCodeValidationRequest;
import com.snoozeshare.auth.security.ClientIpResolver;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.AuthRegistrationService;
import com.snoozeshare.auth.service.AuthRegistrationService.AuthRegistrationException;
import com.snoozeshare.auth.service.AuthRegistrationService.RegistrationResult;
import com.snoozeshare.auth.service.RegistrationCodeValidationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Auth registration and current-profile endpoints. */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final AuthRegistrationService registration;
    private final RegistrationCodeValidationService codeValidation;
    private final ClientIpResolver clientIpResolver;

    public AuthController(
        AuthRegistrationService registration,
        RegistrationCodeValidationService codeValidation,
        ClientIpResolver clientIpResolver) {
        this.registration = registration;
        this.codeValidation = codeValidation;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/registration-code/validate")
    public ResponseEntity<Void> validateRegistrationCode(
        @Valid @RequestBody RegistrationCodeValidationRequest request,
        HttpServletRequest servletRequest) {
        codeValidation.validate(request, clientIpResolver.resolve(servletRequest));
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthProfileResponse> register(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest servletRequest) {
        RegistrationResult result = registration.register(
            identity, request, clientIpResolver.resolve(servletRequest));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.profile());
    }

    @GetMapping("/me")
    public AuthProfileResponse me(@AuthenticationPrincipal FirebaseAuthentication identity) {
        return registration.me(identity);
    }

    @ExceptionHandler(AuthRegistrationException.class)
    ResponseEntity<AuthErrorResponse> handleAuth(
        AuthRegistrationException exception,
        HttpServletRequest request) {
        AuthErrorResponse body = error(exception.code());
        LOGGER.warn("Auth request failed requestId={} status={} code={} path={}",
            body.requestId(), exception.status(), body.code(), request.getRequestURI());
        return noStore(exception.status(), body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<AuthErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        AuthErrorResponse body = error("INTERNAL_ERROR");
        LOGGER.error("Auth request failed requestId={} status=500 code={} path={}",
            body.requestId(), body.code(), request.getRequestURI(), exception);
        return noStore(HttpStatus.INTERNAL_SERVER_ERROR.value(), body);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<AuthErrorResponse> handleValidation(Exception exception, HttpServletRequest request) {
        AuthErrorResponse body = error("VALIDATION_ERROR");
        LOGGER.warn("Auth request failed requestId={} status=400 code={} path={}",
            body.requestId(), body.code(), request.getRequestURI());
        return noStore(HttpStatus.BAD_REQUEST.value(), body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<AuthErrorResponse> handleMediaType(
        HttpMediaTypeNotSupportedException exception,
        HttpServletRequest request) {
        AuthErrorResponse body = error("VALIDATION_ERROR");
        LOGGER.warn("Auth request failed requestId={} status=400 code={} path={}",
            body.requestId(), body.code(), request.getRequestURI());
        return noStore(HttpStatus.BAD_REQUEST.value(), body);
    }

    private AuthErrorResponse error(String code) {
        return new AuthErrorResponse(code, "Request could not be completed", UUID.randomUUID().toString());
    }

    private ResponseEntity<AuthErrorResponse> noStore(int status, AuthErrorResponse body) {
        return ResponseEntity.status(status).header("Cache-Control", "no-store").body(body);
    }
}
