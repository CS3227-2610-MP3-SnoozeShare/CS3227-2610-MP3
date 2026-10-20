package com.snoozeshare.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.auth.repository.AuthUserRepository.AuthUser;
import com.snoozeshare.auth.repository.WalletRepository;
import com.snoozeshare.auth.repository.WalletRepository.Wallet;

/** Performs the atomic first-write portion of registration in its own transaction. */
@Component
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class AuthRegistrationWriter {

    private final AuthUserRepository users;
    private final WalletRepository wallets;

    public AuthRegistrationWriter(AuthUserRepository users, WalletRepository wallets) {
        this.users = users;
        this.wallets = wallets;
    }

    @Transactional
    public AuthUser create(AuthUser user) {
        users.insert(user);
        if (user.role() != AuthRole.AGENT) {
            wallets.insert(new Wallet(java.util.UUID.randomUUID(), user.id(), "SGD", new java.math.BigDecimal("0.00")));
        }
        return user;
    }
}
