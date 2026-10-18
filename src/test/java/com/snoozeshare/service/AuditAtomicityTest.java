package com.snoozeshare.service;

import static com.snoozeshare.service.BookingServiceTest.migratedConnection;
import static com.snoozeshare.service.BookingServiceTest.seedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.TicketCategory;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.infra.db.TransactionManager;
import com.snoozeshare.repository.jdbc.JdbcAvailabilityBlockRepository;
import com.snoozeshare.repository.jdbc.JdbcBookingRepository;
import com.snoozeshare.repository.jdbc.JdbcPropertyRepository;
import com.snoozeshare.repository.jdbc.JdbcReviewRepository;
import com.snoozeshare.repository.jdbc.JdbcTicketCategoryRepository;
import com.snoozeshare.repository.jdbc.JdbcTicketRepository;
import com.snoozeshare.repository.jdbc.JdbcUserRepository;
import com.snoozeshare.repository.jdbc.JdbcWalletRepository;
import com.snoozeshare.service.impl.AvailabilityServiceImpl;
import com.snoozeshare.service.impl.ListingServiceImpl;
import com.snoozeshare.service.impl.ReviewServiceImpl;
import com.snoozeshare.service.impl.TicketServiceImpl;
import com.snoozeshare.service.impl.UserServiceImpl;
import com.snoozeshare.service.requests.NewTicketRequest;
import com.snoozeshare.testsupport.FailingAuditService;
import com.snoozeshare.testsupport.Fakes;
import com.snoozeshare.testsupport.LedgerTestSupport;

/** A failed audit write must roll back the change it was auditing (MP2 feedback D2). */
class AuditAtomicityTest {

    private static Property draftFrom(Property existing, UUID hostId) {
        return new Property(null, hostId, ListingStatus.ACTIVE, "Audit probe", existing.description(),
                existing.propertyType(), existing.streetAddress(), existing.city(), existing.region(),
                existing.postalCode(), existing.maxGuests(), existing.bedrooms(), existing.bathrooms(),
                existing.baseNightlyRate(), existing.checkInTime(), existing.checkOutTime(), existing.amenities(),
                null);
    }

    private static ListingServiceImpl listingService(Connection connection, AuditService audit) {
        var properties = new JdbcPropertyRepository(connection);
        var availability = new AvailabilityServiceImpl(properties, new JdbcAvailabilityBlockRepository(connection),
                new JdbcBookingRepository(connection));
        var users = new UserServiceImpl(connection, new JdbcUserRepository(connection),
                new JdbcWalletRepository(connection));
        return new ListingServiceImpl(new TransactionManager(connection), properties, availability, users, audit);
    }

    @Test
    void failedAuditRollsBackListingCreate() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var properties = new JdbcPropertyRepository(connection);
            int before = properties.findByHostId(ctx.hostId()).size();
            Property draft = draftFrom(properties.findById(ctx.propertyId()).orElseThrow(), ctx.hostId());
            var service = listingService(connection, new FailingAuditService(LedgerTestSupport.audit(connection), 1));

            assertThrows(RuntimeException.class, () -> service.create(draft, ctx.hostId()));

            assertEquals(before, properties.findByHostId(ctx.hostId()).size());
        }
    }

    @Test
    void failedAuditRollsBackListingStatusChange() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var properties = new JdbcPropertyRepository(connection);
            ListingStatus original = properties.findById(ctx.propertyId()).orElseThrow().status();
            ListingStatus other = original == ListingStatus.ACTIVE ? ListingStatus.INACTIVE : ListingStatus.ACTIVE;
            var service = listingService(connection, new FailingAuditService(LedgerTestSupport.audit(connection), 1));

            assertThrows(RuntimeException.class, () -> service.updateStatus(ctx.propertyId(), other, ctx.hostId()));

            assertEquals(original, properties.findById(ctx.propertyId()).orElseThrow().status());
        }
    }

    @Test
    void failedAuditRollsBackReviewSubmit() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var bookings = new JdbcBookingRepository(connection);
            UUID bookingId = UUID.randomUUID();
            bookings.save(new Booking(bookingId, ctx.propertyId(), ctx.guestId(), LocalDate.now().minusDays(20),
                    LocalDate.now().minusDays(15), BookingStatus.COMPLETED, new BigDecimal("100.00"),
                    new BigDecimal("500.00"), Instant.now(), Instant.now(), Instant.now()));
            var reviews = new JdbcReviewRepository(connection);
            var service = new ReviewServiceImpl(new TransactionManager(connection), bookings, reviews,
                    new FailingAuditService(LedgerTestSupport.audit(connection), 1), Clock.systemUTC());

            assertThrows(RuntimeException.class, () -> service.submit(bookingId, ctx.guestId(), 5, "great"));

            assertTrue(reviews.findByBookingId(bookingId).isEmpty());
        }
    }

    @Test
    void failedAuditRollsBackTicketFilingAndCategoryCreation() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            Clock clock = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC);
            var bookings = new JdbcBookingRepository(connection);
            UUID bookingId = UUID.randomUUID();
            bookings.save(new Booking(bookingId, ctx.propertyId(), ctx.guestId(), LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 5), BookingStatus.CONFIRMED, new BigDecimal("100.00"),
                    new BigDecimal("400.00"), Instant.parse("2026-08-20T00:00:00Z"),
                    Instant.parse("2026-08-21T00:00:00Z"), null));
            var categories = new JdbcTicketCategoryRepository(connection);
            categories.save(new TicketCategory(UUID.randomUUID(), "Cleanliness", true));
            var users = new JdbcUserRepository(connection);
            User agent = new User(UUID.randomUUID(), Role.AGENT, "Agent", "agent@example.com", AccountStatus.ACTIVE,
                    null, Instant.parse("2026-01-01T00:00:00Z"));
            users.save(agent);
            var tickets = new JdbcTicketRepository(connection);
            var service = new TicketServiceImpl(new TransactionManager(connection), tickets, categories, bookings,
                    users, new Fakes.RecordingSettlement(),
                    new FailingAuditService(LedgerTestSupport.audit(connection), 1), clock);

            assertThrows(RuntimeException.class, () -> service.fileTicket(new NewTicketRequest(bookingId,
                    "Cleanliness", "Dirty", "Not cleaned", RemedyType.FULL_REFUND, null), ctx.guestId(), Role.GUEST));
            assertTrue(tickets.findByRaisedByUserId(ctx.guestId()).isEmpty());

            var failingAgentService = new TicketServiceImpl(new TransactionManager(connection), tickets, categories,
                    bookings, users, new Fakes.RecordingSettlement(),
                    new FailingAuditService(LedgerTestSupport.audit(connection), 1), clock);
            int before = categories.findAll().size();
            assertThrows(RuntimeException.class, () -> failingAgentService.createCategory("Noise", agent.userId()));
            assertEquals(before, categories.findAll().size());
        }
    }
}
