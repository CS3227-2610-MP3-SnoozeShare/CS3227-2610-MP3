package com.snoozeshare.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    private UserRepository userRepository;

    private User guest;
    private User host;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        guest = userRepository.save(new User(
                UUID.randomUUID(), "fb-guest", Role.GUEST,
                "Alice", "alice@example.com",
                AccountStatus.ACTIVE, null, now, now, null));
        host = userRepository.save(new User(
                UUID.randomUUID(), "fb-host", Role.HOST,
                "Bob", "bob@example.com",
                AccountStatus.ACTIVE, "HOST123", now, now, null));
    }

    @Test
    void findByEmailReturnsUser() {
        Optional<User> found = userRepository.findByEmail("alice@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(guest.getId());
    }

    @Test
    void findByEmailReturnsEmptyForUnknown() {
        assertThat(userRepository.findByEmail("unknown@example.com")).isEmpty();
    }

    @Test
    void findByRoleReturnsMatchingUsers() {
        List<User> guests = userRepository.findByRole(Role.GUEST);
        assertThat(guests).hasSize(1);
        assertThat(guests.getFirst().getId()).isEqualTo(guest.getId());
    }

    @Test
    void findAllByOrderByCreatedAtAscIdAscReturnsSorted() {
        List<User> all = userRepository.findAllByOrderByCreatedAtAscIdAsc();
        // V004 seeds a SYSTEM user, so at least 3 (SYSTEM + guest + host)
        assertThat(all).hasSizeGreaterThanOrEqualTo(3);
    }
}
