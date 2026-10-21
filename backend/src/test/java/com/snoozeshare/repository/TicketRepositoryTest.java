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
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.User;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketRepositoryTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    private User guest;
    private User host;
    private Booking booking;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        guest = userRepository.save(new User(
                UUID.randomUUID(), "fb-guest", Role.GUEST,
                "Alice", "alice@tickets.test",
                AccountStatus.ACTIVE, null, now, now, null));
        host = userRepository.save(new User(
                UUID.randomUUID(), "fb-host", Role.HOST,
                "Bob", "bob@tickets.test",
                AccountStatus.ACTIVE, "HOST123", now, now, null));
        Property property = propertyRepository.save(new Property(
                UUID.randomUUID(), host.getId(),
                ListingStatus.ACTIVE, "Test Place",
                "Test description", PropertyType.APARTMENT,
                "1 Test St", "Singapore", "Central",
                100000, 2, 1, 1,
                new BigDecimal("80.0000"),
                LocalTime.of(14, 0), LocalTime.of(11, 0),
                Set.of(), now));
        booking = bookingRepository.save(new Booking(
                UUID.randomUUID(), property.getId(), guest.getId(),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5),
                BookingStatus.CONFIRMED,
                new BigDecimal("80.0000"), new BigDecimal("320.0000"),
                now, now, null, null));
    }

    private Ticket saveTicket(TicketStatus status) {
        return ticketRepository.save(new Ticket(
                UUID.randomUUID(), booking.getId(),
                guest.getId(), Role.GUEST,
                "Cleanliness", "Dirty room",
                "The room was dirty", RemedyType.FULL_REFUND,
                null, status, null, null, null,
                Instant.now(), null));
    }

    @Test
    void findByStatusReturnsMatchingTickets() {
        saveTicket(TicketStatus.OPEN);
        saveTicket(TicketStatus.IN_REVIEW);

        List<Ticket> open = ticketRepository.findByStatus(TicketStatus.OPEN);
        assertThat(open).hasSize(1);
    }

    @Test
    void findByBookingIdReturnsTicketsForBooking() {
        saveTicket(TicketStatus.OPEN);

        List<Ticket> found = ticketRepository.findByBookingId(booking.getId());
        assertThat(found).hasSize(1);
    }

    @Test
    void existsByCategoryIsCaseInsensitive() {
        saveTicket(TicketStatus.OPEN);

        assertThat(ticketRepository.existsByCategory("cleanliness")).isTrue();
        assertThat(ticketRepository.existsByCategory("CLEANLINESS")).isTrue();
        assertThat(ticketRepository.existsByCategory("Unknown")).isFalse();
    }

    @Test
    void findByGuestReturnsTicketsOnGuestBookings() {
        saveTicket(TicketStatus.OPEN);

        List<Ticket> found = ticketRepository.findByGuest(guest.getId());
        assertThat(found).hasSize(1);
    }

    @Test
    void findByHostReturnsTicketsOnHostProperties() {
        saveTicket(TicketStatus.OPEN);

        List<Ticket> found = ticketRepository.findByHost(host.getId());
        assertThat(found).hasSize(1);
    }
}
