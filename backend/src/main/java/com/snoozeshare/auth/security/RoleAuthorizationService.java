package com.snoozeshare.auth.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;

/** Resolves the server-owned active role for an authenticated Firebase identity. */
@Service
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class RoleAuthorizationService {

    private final AuthUserRepository users;

    public RoleAuthorizationService(AuthUserRepository users) {
        this.users = users;
    }

    public AuthUserRepository.AuthUser requireActiveUser(FirebaseAuthentication identity)
            throws AuthorizationException {
        AuthUserRepository.AuthUser user = users.findByFirebaseUid(identity.uid())
            .orElseThrow(() -> new AuthorizationException("PROFILE_NOT_REGISTERED"));
        if (!"ACTIVE".equals(user.status())) {
            throw new AuthorizationException("PROFILE_INACTIVE");
        }
        return user;
    }

    public void requireRole(AuthRole actual, AuthRole expected) throws AuthorizationException {
        if (actual != expected) {
            throw new AuthorizationException("ROLE_MISMATCH");
        }
    }

    public static class AuthorizationException extends Exception {

        private final String code;

        public AuthorizationException(String code) {
            super(code);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
