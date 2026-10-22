package com.snoozeshare.testsupport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;

/** Creates database-backed users and matching fake bearer tokens for API tests. */
public class AgentTestUsers {

    private final UserRepository users;
    private final WalletRepository wallets;
    private final FakeTokenVerifier verifier;

    public AgentTestUsers(UserRepository users, WalletRepository wallets,
                          FakeTokenVerifier verifier) {
        this.users = users;
        this.wallets = wallets;
        this.verifier = verifier;
    }

    public TestUser create(Role role) {
        UUID id = UUID.randomUUID();
        String token = "agent-api-" + role.name().toLowerCase() + "-" + id;
        String uid = "firebase-" + id;
        String email = role.name().toLowerCase() + "+" + id + "@api.test";
        Instant now = Instant.now();
        User user = users.save(new User(id, uid, role, role.name(), email,
                AccountStatus.ACTIVE, null, now, now, null));
        wallets.save(new Wallet(UUID.randomUUID(), id, BigDecimal.ZERO, "SGD", now));
        verifier.add(token, uid, email);
        return new TestUser(user, token);
    }

    public record TestUser(User user, String token) {
        public UUID id() {
            return user.getId();
        }
    }
}
