package com.snoozeshare.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.snoozeshare.auth.api.AuthProfileResponse;
import com.snoozeshare.auth.api.RegisterRequest;
import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.auth.repository.AuthUserRepository.AuthUser;
import com.snoozeshare.auth.repository.WalletRepository;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;

/** Transactional registration and profile lookup service. */
@Service
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class AuthRegistrationService {

    private final AuthUserRepository users;
    private final WalletRepository wallets;
    private final AuthRegistrationWriter writer;
    private final RegistrationRateLimiter rateLimiter;
    private final String hostCode;
    private final String agentCode;

    public AuthRegistrationService(
        AuthUserRepository users,
        WalletRepository wallets,
        AuthRegistrationWriter writer,
        RegistrationRateLimiter rateLimiter,
        @Value("${snoozeshare.registration.host-code:}") String hostCode,
        @Value("${snoozeshare.registration.agent-code:}") String agentCode) {
        this.users = users;
        this.wallets = wallets;
        this.writer = writer;
        this.rateLimiter = rateLimiter;
        this.hostCode = hostCode;
        this.agentCode = agentCode;
    }

    public RegistrationResult register(FirebaseAuthentication identity, RegisterRequest request, String ip) {
        validateCode(identity, request, ip);
        AuthUser existing = users.findByFirebaseUid(identity.uid()).orElse(null);
        if (existing != null) {
            if (!"ACTIVE".equals(existing.status())) {
                throw new AuthRegistrationException("PROFILE_INACTIVE", 403);
            }
            if (existing.role() != request.role()) {
                throw new AuthRegistrationException("ROLE_ALREADY_ASSIGNED", 409);
            }
            return new RegistrationResult(profile(existing), false);
        }

        AuthUser created = new AuthUser(
            UUID.randomUUID(),
            identity.uid(),
            normalizeEmail(identity.email()),
            request.role(),
            normalizeDisplayName(request.displayName()),
            "ACTIVE",
            null,
            null);
        try {
            writer.create(created);
        } catch (DataIntegrityViolationException conflict) {
            AuthUser winner = users.findByFirebaseUid(identity.uid()).orElseThrow(() -> conflict);
            if (winner.role() != request.role()) {
                throw new AuthRegistrationException("ROLE_ALREADY_ASSIGNED", 409);
            }
            return new RegistrationResult(profile(winner), false);
        }
        if (request.role() != AuthRole.GUEST) {
            rateLimiter.resetUser(identity.uid(), ip);
        }
        return new RegistrationResult(profile(created), true);
    }

    public AuthProfileResponse me(FirebaseAuthentication identity) {
        AuthUser user = users.findByFirebaseUid(identity.uid())
            .orElseThrow(() -> new AuthRegistrationException("PROFILE_NOT_REGISTERED", 404));
        if (!"ACTIVE".equals(user.status())) {
            throw new AuthRegistrationException("PROFILE_INACTIVE", 403);
        }
        return profile(user);
    }

    private AuthProfileResponse profile(AuthUser user) {
        return AuthProfileResponse.from(user, wallets.findByUserId(user.id()).orElse(null));
    }

    public record RegistrationResult(AuthProfileResponse profile, boolean created) { }

    private void validateCode(FirebaseAuthentication identity, RegisterRequest request, String ip) {
        String submitted = request.registrationCode();
        if (request.role() == AuthRole.GUEST) {
            if (submitted != null && !submitted.isBlank()) {
                throw new AuthRegistrationException("VALIDATION_ERROR", 400);
            }
            return;
        }
        String expected = request.role() == AuthRole.HOST ? hostCode : agentCode;
        if (!rateLimiter.allow(identity.uid(), ip)) {
            throw new AuthRegistrationException("REGISTRATION_RATE_LIMITED", 429);
        }
        if (submitted == null || !MessageDigest.isEqual(
                submitted.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            rateLimiter.recordInvalid(identity.uid(), ip);
            throw new AuthRegistrationException("INVALID_REGISTRATION_CODE", 403);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AuthRegistrationException("VALIDATION_ERROR", 400);
        }
        return email.toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        String normalized = displayName.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        boolean hasControl = normalized.codePoints().anyMatch(Character::isISOControl);
        boolean tooLong = normalized.codePointCount(0, normalized.length()) > 80;
        if (hasControl || tooLong) {
            throw new AuthRegistrationException("VALIDATION_ERROR", 400);
        }
        return normalized;
    }

    public static class AuthRegistrationException extends RuntimeException {

        private final String code;
        private final int status;

        public AuthRegistrationException(String code, int status) {
            super(code);
            this.code = code;
            this.status = status;
        }

        public String code() {
            return code;
        }

        public int status() {
            return status;
        }
    }
}
