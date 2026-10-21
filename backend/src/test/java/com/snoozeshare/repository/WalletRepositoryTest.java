package com.snoozeshare.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletRepositoryTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        user = userRepository.save(new User(
                UUID.randomUUID(), "fb-user", Role.GUEST,
                "Charlie", "charlie@test.com",
                AccountStatus.ACTIVE, null, now, now, null));
    }

    @Test
    void findByUserIdReturnsWallet() {
        Wallet wallet = walletRepository.save(new Wallet(
                UUID.randomUUID(), user.getId(),
                new BigDecimal("500.00"), "SGD", Instant.now()));

        Optional<Wallet> found = walletRepository.findByUserId(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void findByUserIdReturnsEmptyForUnknown() {
        assertThat(walletRepository.findByUserId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void balanceUpdates() {
        Wallet wallet = walletRepository.save(new Wallet(
                UUID.randomUUID(), user.getId(),
                new BigDecimal("100.00"), "SGD", Instant.now()));

        wallet.setBalance(new BigDecimal("250.00"));
        walletRepository.save(wallet);

        Optional<Wallet> found = walletRepository.findByUserId(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getBalance()).isEqualByComparingTo("250.00");
    }
}
