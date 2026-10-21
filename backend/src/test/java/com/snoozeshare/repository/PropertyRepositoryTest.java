package com.snoozeshare.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;

/** Verifies the Host listing query is owner-scoped, deterministic, and database-bounded. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PropertyRepositoryTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    private PropertyRepository properties;

    @Autowired
    private UserRepository users;

    private UUID hostId;
    private UUID otherHostId;
    private List<Property> orderedHostProperties;

    @BeforeEach
    void setUp() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        hostId = UUID.randomUUID();
        otherHostId = UUID.randomUUID();
        users.save(user(hostId, "host"));
        users.save(user(otherHostId, "other-host"));

        Instant older = now.minusSeconds(10);
        Property old = property(UUID.randomUUID(), hostId, older, "Old");
        Property newestHighId = property(
            UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), hostId, now, "Newest high");
        Property newestLowId = property(
            UUID.fromString("00000000-0000-0000-0000-000000000001"), hostId, now, "Newest low");
        orderedHostProperties = List.of(newestLowId, newestHighId, old);
        properties.saveAll(List.of(old, newestHighId, newestLowId,
            property(UUID.randomUUID(), hostId, older.minusSeconds(10), "Fourth"),
            property(UUID.randomUUID(), otherHostId, now.plusSeconds(10), "Other host")));
    }

    @Test
    void hostPageReadsAtMostSizePlusOneRowsInStableOwnerScopedOrder() {
        List<Property> fetched = properties.findByHostIdOrderByCreatedAtDescIdAsc(
            hostId, new OffsetLimitPageable(0, 2));

        assertThat(fetched).hasSize(3)
            .extracting(Property::getId)
            .containsExactlyElementsOf(orderedHostProperties.stream().map(Property::getId).toList());
        assertThat(fetched).allMatch(property -> property.getHostId().equals(hostId));
    }

    @Test
    void adjacentHostPagesDoNotSkipOrRepeatRows() {
        List<Property> firstPage = properties.findByHostIdOrderByCreatedAtDescIdAsc(
            hostId, new OffsetLimitPageable(0, 2));
        List<Property> secondPage = properties.findByHostIdOrderByCreatedAtDescIdAsc(
            hostId, new OffsetLimitPageable(1, 2));

        assertThat(firstPage).extracting(Property::getId)
            .containsExactly(orderedHostProperties.get(0).getId(), orderedHostProperties.get(1).getId(),
                orderedHostProperties.get(2).getId());
        assertThat(secondPage).extracting(Property::getId)
            .containsExactly(orderedHostProperties.get(2).getId(),
                properties.findByHostId(hostId).stream()
                    .filter(property -> !orderedHostProperties.stream()
                        .map(Property::getId).toList().contains(property.getId()))
                    .findFirst().orElseThrow().getId());
    }

    private User user(UUID id, String suffix) {
        return new User(id, "firebase-" + suffix, Role.HOST, suffix, suffix + "@properties.test",
            AccountStatus.ACTIVE, null, Instant.EPOCH, Instant.EPOCH, null);
    }

    private Property property(UUID id, UUID ownerId, Instant createdAt, String title) {
        return new Property(id, ownerId, ListingStatus.ACTIVE, title, "Description",
            PropertyType.APARTMENT, "1 Main St", "Singapore", "Central", 123456,
            2, 1, 1, new BigDecimal("80.00"), LocalTime.of(15, 0), LocalTime.of(11, 0),
            Set.of(), createdAt);
    }
}
