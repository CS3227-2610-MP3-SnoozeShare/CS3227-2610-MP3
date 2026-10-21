package com.snoozeshare.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    private User guest;
    private User host;
    private Property property;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        guest = userRepository.save(new User(
                UUID.randomUUID(), "fb-guest", Role.GUEST,
                "Alice", "alice@test.com",
                AccountStatus.ACTIVE, null, now, now, null));
        host = userRepository.save(new User(
                UUID.randomUUID(), "fb-host", Role.HOST,
                "Bob", "bob@test.com",
                AccountStatus.ACTIVE, "HOST123", now, now, null));
        property = propertyRepository.save(new Property(
                UUID.randomUUID(), host.getId(),
                ListingStatus.ACTIVE, "Nice Place",
                "A very nice place", PropertyType.APARTMENT,
                "123 Main St", "Singapore", "Central",
                123456, 2, 1, 1,
                new BigDecimal("100.0000"),
                LocalTime.of(14, 0), LocalTime.of(11, 0),
                Set.of(), now));
    }

    private Booking saveBooking(LocalDate start, LocalDate end,
                                BookingStatus status) {
        return bookingRepository.save(new Booking(
                UUID.randomUUID(), property.getId(), guest.getId(),
                start, end, status,
                new BigDecimal("100.0000"), new BigDecimal("300.0000"),
                Instant.now(), null, null, null));
    }

    @Test
    void findOverlappingExcludesCancelledBookings() {
        saveBooking(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                BookingStatus.CONFIRMED);
        saveBooking(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 8),
                BookingStatus.CANCELLED_BY_GUEST);

        List<Booking> overlapping = bookingRepository.findOverlapping(
                property.getId(),
                LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 6));
        assertThat(overlapping).hasSize(1);
        assertThat(overlapping.getFirst().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void findOverlappingReturnsEmptyWhenNoOverlap() {
        saveBooking(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5),
                BookingStatus.CONFIRMED);

        List<Booking> overlapping = bookingRepository.findOverlapping(
                property.getId(),
                LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 10));
        assertThat(overlapping).isEmpty();
    }

    @Test
    void findByGuestIdReturnsGuestBookings() {
        saveBooking(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                BookingStatus.PENDING);

        List<Booking> found = bookingRepository.findByGuestId(guest.getId());
        assertThat(found).hasSize(1);
    }

    @Test
    void findByHostPendingReturnsOnlyPendingForHost() {
        saveBooking(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                BookingStatus.PENDING);
        saveBooking(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 15),
                BookingStatus.CONFIRMED);

        List<Booking> pending = bookingRepository.findByHostPending(host.getId());
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void findByHostReturnsAllForHost() {
        saveBooking(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5),
                BookingStatus.PENDING);
        saveBooking(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15),
                BookingStatus.CONFIRMED);

        List<Booking> all = bookingRepository.findByHost(host.getId());
        assertThat(all).hasSize(2);
    }

    @Test
    void findConfirmedEndingOnOrBefore() {
        saveBooking(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5),
                BookingStatus.CONFIRMED);
        saveBooking(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 20),
                BookingStatus.CONFIRMED);

        List<Booking> found = bookingRepository.findConfirmedEndingOnOrBefore(
                LocalDate.of(2026, 6, 10));
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getEndDate()).isEqualTo(LocalDate.of(2026, 6, 5));
    }
}
