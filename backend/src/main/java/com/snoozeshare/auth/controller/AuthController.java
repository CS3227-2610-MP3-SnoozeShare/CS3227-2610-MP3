package com.snoozeshare.auth.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.AuthRegistrationService;
import com.snoozeshare.auth.service.AuthRegistrationService.AuthRegistrationException;
import com.snoozeshare.auth.service.AuthRegistrationService.RegistrationResult;

import jakarta.validation.Valid;

/** Auth registration and current-profile endpoints. */
@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class AuthController {

    private final AuthRegistrationService registration;

    public AuthController(AuthRegistrationService registration) {
        this.registration = registration;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthProfileResponse> register(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @Valid @RequestBody RegisterRequest request,
        jakarta.servlet.http.HttpServletRequest servletRequest) {
        RegistrationResult result = registration.register(identity, request, servletRequest.getRemoteAddr());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.profile());
    }

    @GetMapping("/me")
    public AuthProfileResponse me(@AuthenticationPrincipal FirebaseAuthentication identity) {
        return registration.me(identity);
    }

    @ExceptionHandler(AuthRegistrationException.class)
    ResponseEntity<AuthErrorResponse> handleAuth(AuthRegistrationException exception) {
        return ResponseEntity.status(exception.status()).body(error(exception.code()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<AuthErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error("VALIDATION_ERROR"));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<AuthErrorResponse> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR"));
    }

    private AuthErrorResponse error(String code) {
        return new AuthErrorResponse(code, "Request could not be completed", UUID.randomUUID().toString());
    }
}
