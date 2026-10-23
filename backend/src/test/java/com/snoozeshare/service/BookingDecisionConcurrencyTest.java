package com.snoozeshare.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.containers.PostgreSQLContainer;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.AuditLogEntry;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.infra.events.BookingCancelledEvent;
import com.snoozeshare.infra.events.BookingConfirmedEvent;
import com.snoozeshare.repository.AuditLogRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;

/** E7/E19: concurrent Host decisions must serialize on one booking row. */
@SpringBootTest
@RecordApplicationEvents
class BookingDecisionConcurrencyTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookings;
    @Autowired private PropertyRepository properties;
    @Autowired private UserRepository users;
    @Autowired private WalletRepository wallets;
    @Autowired private AuditLogRepository audits;
    @Autowired private ApplicationEvents applicationEvents;

    private UUID hostId;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        users.save(new User(hostId, "host-" + hostId, Role.HOST, "Host",
            hostId + "@example.com", AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
        users.save(new User(guestId, "guest-" + guestId, Role.GUEST, "Guest",
            guestId + "@example.com", AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
        wallets.save(new Wallet(UUID.randomUUID(), guestId, BigDecimal.ZERO, "SGD", Instant.now()));
        properties.save(new Property(listingId, hostId, ListingStatus.ACTIVE, "Home", "Description",
            PropertyType.APARTMENT, "1 Main St", "Singapore", "Central", 123456, 2, 1, 1,
            new BigDecimal("80.00"), LocalTime.of(15, 0), LocalTime.of(11, 0), Set.of(), Instant.now()));
        bookingId = UUID.randomUUID();
        bookings.save(new Booking(bookingId, listingId, guestId, LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(32), BookingStatus.PENDING, new BigDecimal("80.00"),
            new BigDecimal("160.00"), Instant.now(), null, null, null));
    }

    @Test
    void exactlyOneConcurrentDecisionWinsAndOnlyOneStateIsCommitted() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<String>> outcomes = List.of(
                executor.submit(() -> decideAfter(start, true)),
                executor.submit(() -> decideAfter(start, false)));
            start.countDown();
            assertThat(outcomes.stream().map(this::outcome).toList())
                .containsExactlyInAnyOrder("won", "conflict");
        }
        assertThat(bookings.findById(bookingId).orElseThrow().getStatus())
            .isIn(BookingStatus.CONFIRMED, BookingStatus.REJECTED);
        List<AuditLogEntry> decisionAudits = audits.findAll().stream()
            .filter(row -> bookingId.equals(row.getBookingId()))
            .filter(row -> "BOOKING_CONFIRMED".equals(row.getActionType())
                || "BOOKING_REJECTED".equals(row.getActionType()))
            .toList();
        assertThat(decisionAudits).hasSize(1);
        long confirmedEvents = applicationEvents.stream(BookingConfirmedEvent.class)
            .filter(event -> bookingId.equals(event.bookingId())).count();
        long cancelledEvents = applicationEvents.stream(BookingCancelledEvent.class)
            .filter(event -> bookingId.equals(event.bookingId())).count();
        assertThat(confirmedEvents + cancelledEvents).isEqualTo(1);
    }

    private String decideAfter(CountDownLatch start, boolean approve) throws Exception {
        start.await();
        try {
            bookingService.decide(bookingId, approve, hostId);
            return "won";
        } catch (IllegalStateException exception) {
            return "conflict";
        }
    }

    private String outcome(Future<String> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
