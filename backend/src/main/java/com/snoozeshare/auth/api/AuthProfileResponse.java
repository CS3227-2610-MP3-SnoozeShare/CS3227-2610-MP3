package com.snoozeshare.auth.api;

import java.util.UUID;

import com.snoozeshare.auth.repository.AuthUserRepository.AuthUser;
import com.snoozeshare.auth.repository.WalletRepository.Wallet;

/** Public profile response shared by registration and {@code /me}. */
public record AuthProfileResponse(
    UUID id,
    String email,
    String role,
    String displayName,
    UUID walletId) {

    public static AuthProfileResponse from(AuthUser user, Wallet wallet) {
        return new AuthProfileResponse(
            user.id(),
            user.email(),
            user.role().name(),
            user.displayName(),
            wallet == null ? null : wallet.id());
    }
}
