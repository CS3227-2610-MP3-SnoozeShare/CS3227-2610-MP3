package com.snoozeshare.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.auth.repository.AuthUserRepository.AuthUser;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.UserRepository;

class HostIdentityResolverTest {

    private static final String FIREBASE_UID = "firebase-host-uid";
    private static final UUID HOST_ID = UUID.randomUUID();

    private final AuthUserRepository authUsers = mock(AuthUserRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final HostIdentityResolver resolver = new HostIdentityResolver(authUsers, users);

    @Test
    void resolvesMatchingActiveHostFromVerifiedFirebasePrincipal() {
        when(authUsers.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.of(authHost()));
        when(users.findById(HOST_ID)).thenReturn(Optional.of(domainHost()));

        UUID resolved = resolver.resolve(new FirebaseAuthentication(FIREBASE_UID, "host@example.com", true));

        assertThat(resolved).isEqualTo(HOST_ID);
    }

    @Test
    void rejectsMissingDomainUserAsProfileInconsistent() {
        when(authUsers.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.of(authHost()));
        when(users.findById(HOST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolve(new FirebaseAuthentication(FIREBASE_UID, null, true)))
            .isInstanceOf(HostApiException.class)
            .hasMessage("PROFILE_INCONSISTENT")
            .extracting(exception -> ((HostApiException) exception).code())
            .isEqualTo("PROFILE_INCONSISTENT");
    }

    @Test
    void rejectsDomainUserWithMismatchedUidRoleOrStatus() {
        when(authUsers.findByFirebaseUid(FIREBASE_UID)).thenReturn(Optional.of(authHost()));
        when(users.findById(HOST_ID)).thenReturn(Optional.of(new User(
            HOST_ID, FIREBASE_UID, Role.GUEST, "Host", "host@example.com", AccountStatus.SUSPENDED,
            null, Instant.EPOCH, Instant.EPOCH, null)));

        assertThatThrownBy(() -> resolver.resolve(new FirebaseAuthentication(FIREBASE_UID, null, true)))
            .isInstanceOf(HostApiException.class)
            .extracting(exception -> ((HostApiException) exception).code())
            .isEqualTo("PROFILE_INCONSISTENT");
    }

    private AuthUser authHost() {
        return new AuthUser(HOST_ID, FIREBASE_UID, "host@example.com", AuthRole.HOST,
            "Host", "ACTIVE", OffsetDateTime.now(), OffsetDateTime.now());
    }

    private User domainHost() {
        return new User(HOST_ID, FIREBASE_UID, Role.HOST, "Host", "host@example.com",
            AccountStatus.ACTIVE, null, Instant.EPOCH, Instant.EPOCH, null);
    }
}
