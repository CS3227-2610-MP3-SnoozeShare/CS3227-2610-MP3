package com.snoozeshare.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.snoozeshare.auth.api.RegistrationCodeValidationRequest;
import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.service.AuthRegistrationService.AuthRegistrationException;

/** Validates privileged registration codes before Firebase signup. */
@Service
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class RegistrationCodeValidationService {

    private final PublicRegistrationRateLimiter rateLimiter;
    private final String hostCode;
    private final String agentCode;

    public RegistrationCodeValidationService(
        PublicRegistrationRateLimiter rateLimiter,
        @Value("${snoozeshare.registration.host-code:}") String hostCode,
        @Value("${snoozeshare.registration.agent-code:}") String agentCode) {
        this.rateLimiter = rateLimiter;
        this.hostCode = hostCode;
        this.agentCode = agentCode;
    }

    public void validate(RegistrationCodeValidationRequest request, String ip) {
        if (request.role() == AuthRole.GUEST) {
            throw new AuthRegistrationException("VALIDATION_ERROR", 400);
        }
        String expected = request.role() == AuthRole.HOST ? hostCode : agentCode;
        boolean matches = MessageDigest.isEqual(
            request.registrationCode().getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8));
        if (matches) {
            rateLimiter.resetRole(ip, request.role());
            return;
        }
        if (!rateLimiter.recordInvalid(ip, request.role())) {
            throw new AuthRegistrationException("REGISTRATION_RATE_LIMITED", 429);
        }
        throw new AuthRegistrationException("INVALID_REGISTRATION_CODE", 403);
    }
}
