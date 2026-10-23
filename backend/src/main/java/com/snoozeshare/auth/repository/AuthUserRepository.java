package com.snoozeshare.auth.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.snoozeshare.auth.model.AuthRole;

/** Persistence boundary for the Firebase-backed application profile. */
public interface AuthUserRepository {

    Optional<AuthUser> findByFirebaseUid(String firebaseUid);

    Optional<AuthUser> findById(UUID id);

    AuthUser insert(AuthUser user);

    /** Immutable identity and role fields are intentionally represented together. */
    record AuthUser(
        UUID id,
        String firebaseUid,
        String email,
        AuthRole role,
        String displayName,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) { }
}
