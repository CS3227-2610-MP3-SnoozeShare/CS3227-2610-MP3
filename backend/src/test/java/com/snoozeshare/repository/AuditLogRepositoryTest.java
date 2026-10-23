package com.snoozeshare.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
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
import com.snoozeshare.domain.model.AuditLogEntry;
import com.snoozeshare.domain.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditLogRepositoryTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    private User actor;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        actor = userRepository.save(new User(
                UUID.randomUUID(), "fb-actor", Role.AGENT,
                "Agent Smith", "agent@test.com",
                AccountStatus.ACTIVE, "AGENT123", now, now, null));
    }

    @Test
    void saveAndRetrieveEntry() {
        UUID entityId = UUID.randomUUID();
        AuditLogEntry entry = auditLogRepository.save(new AuditLogEntry(
                UUID.randomUUID(), actor.getId(),
                "Agent Smith", "BOOKING_APPROVED",
                "BOOKING", entityId,
                "PENDING", "CONFIRMED",
                null, "Host approved", null, null,
                null, null, Instant.now(), null));

        assertThat(auditLogRepository.findById(entry.getId())).isPresent();
    }

    @Test
    void saveMoneyRow() {
        UUID walletEntityId = UUID.randomUUID();
        AuditLogEntry entry = auditLogRepository.save(new AuditLogEntry(
                UUID.randomUUID(), actor.getId(),
                "Agent Smith", "TOP_UP",
                "WALLET", walletEntityId,
                null, null,
                new BigDecimal("100.0000"), "Top up",
                actor.getId(), "Agent Smith",
                null, null, Instant.now(),
                new BigDecimal("600.0000")));

        AuditLogEntry found = auditLogRepository.findById(entry.getId()).orElseThrow();
        assertThat(found.getWalletAdjustment()).isEqualByComparingTo("100.0000");
        assertThat(found.getBalanceAfter()).isEqualByComparingTo("600.0000");
    }
}
