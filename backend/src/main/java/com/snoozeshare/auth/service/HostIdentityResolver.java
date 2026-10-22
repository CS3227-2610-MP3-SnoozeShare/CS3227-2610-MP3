package com.snoozeshare.auth.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.UserRepository;

/** Resolves and cross-checks the active Host represented by the verified Firebase principal. */
@Service
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostIdentityResolver {

    private final AuthUserRepository authUsers;
    private final UserRepository users;

    public HostIdentityResolver(AuthUserRepository authUsers, UserRepository users) {
        this.authUsers = authUsers;
        this.users = users;
    }

    public UUID resolve(FirebaseAuthentication identity) {
        if (identity == null || identity.uid() == null || identity.uid().isBlank()) {
            throw new HostApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
        }
        AuthUserRepository.AuthUser authUser = authUsers.findByFirebaseUid(identity.uid())
            .orElseThrow(() -> new HostApiException(HttpStatus.NOT_FOUND, "PROFILE_NOT_REGISTERED"));
        if (authUser.role() != AuthRole.HOST || !"ACTIVE".equals(authUser.status())
            || !identity.uid().equals(authUser.firebaseUid())) {
            throw profileInconsistent();
        }

        User domainUser = users.findById(authUser.id()).orElseThrow(HostIdentityResolver::profileInconsistent);
        if (!authUser.id().equals(domainUser.getId())
            || !identity.uid().equals(domainUser.getFirebaseUid())
            || domainUser.getRole() != Role.HOST
            || domainUser.getStatus() != AccountStatus.ACTIVE) {
            throw profileInconsistent();
        }
        return domainUser.getId();
    }

    private static HostApiException profileInconsistent() {
        return new HostApiException(HttpStatus.CONFLICT, "PROFILE_INCONSISTENT");
    }
}
