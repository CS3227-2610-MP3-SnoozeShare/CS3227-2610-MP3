package com.snoozeshare.service;

import static com.snoozeshare.service.BookingServiceTest.migratedConnection;
import static com.snoozeshare.service.BookingServiceTest.seedContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Ticket;
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
    void failedAuditRollsBackListingUpdate() throws Exception {
        try (Connection connection = migratedConnection()) {
            var ctx = seedContext(connection, new BigDecimal("500.00"));
            var properties = new JdbcPropertyRepository(connection);
            Property existing = properties.findById(ctx.propertyId()).orElseThrow();
            Property edit = new Property(existing.propertyId(), ctx.hostId(), existing.status(), "Retitled",
                    existing.description(), existing.propertyType(), existing.streetAddress(), existing.city(),
                    existing.region(), existing.postalCode(), existing.maxGuests(), existing.bedrooms(),
                    existing.bathrooms(), existing.baseNightlyRate(), existing.checkInTime(),
                    existing.checkOutTime(), existing.amenities(), existing.createdAt());
            var service = listingService(connection, new FailingAuditService(LedgerTestSupport.audit(connection), 1));

            assertThrows(RuntimeException.class, () -> service.update(edit, ctx.hostId()));

            assertEquals(existing.title(), properties.findById(ctx.propertyId()).orElseThrow().title());
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

    /** Real repositories plus a guest booking, an active category and an agent; services are built per audit. */
    private static final class TicketFixture {
        final Connection connection;
        final BookingServiceTest.TestContext ctx;
        final UUID bookingId = UUID.randomUUID();
        final JdbcTicketCategoryRepository categories;
        final JdbcTicketRepository tickets;
        final User agent;
        private final JdbcBookingRepository bookings;
        private final JdbcUserRepository users;
        private final Clock clock = Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC);

        TicketFixture(Connection connection) {
            this.connection = connection;
            this.ctx = seedContext(connection, new BigDecimal("500.00"));
            bookings = new JdbcBookingRepository(connection);
            bookings.save(new Booking(bookingId, ctx.propertyId(), ctx.guestId(), LocalDate.of(2026, 9, 1),
                    LocalDate.of(2026, 9, 5), BookingStatus.CONFIRMED, new BigDecimal("100.00"),
                    new BigDecimal("400.00"), Instant.parse("2026-08-20T00:00:00Z"),
                    Instant.parse("2026-08-21T00:00:00Z"), null));
            categories = new JdbcTicketCategoryRepository(connection);
            categories.save(new TicketCategory(UUID.randomUUID(), "Cleanliness", true));
            users = new JdbcUserRepository(connection);
            agent = new User(UUID.randomUUID(), Role.AGENT, "Agent", "agent@example.com", AccountStatus.ACTIVE,
                    null, Instant.parse("2026-01-01T00:00:00Z"));
            users.save(agent);
            tickets = new JdbcTicketRepository(connection);
        }

        TicketServiceImpl service(AuditService audit) {
            return new TicketServiceImpl(new TransactionManager(connection), tickets, categories, bookings, users,
                    new Fakes.RecordingSettlement(), audit, clock);
        }

        TicketServiceImpl failingService() {
            return service(new FailingAuditService(LedgerTestSupport.audit(connection), 1));
        }

        TicketServiceImpl healthyService() {
            return service(LedgerTestSupport.audit(connection));
        }

        Ticket openTicket() {
            return healthyService().fileTicket(new NewTicketRequest(bookingId, "Cleanliness", "Dirty",
                    "Not cleaned", RemedyType.FULL_REFUND, null), ctx.guestId(), Role.GUEST);
        }

        TicketCategory newCategory(String label) {
            return healthyService().createCategory(label, agent.userId());
        }
    }

    @Test
    void failedAuditRollsBackTicketFiling() throws Exception {
        try (Connection connection = migratedConnection()) {
            var fixture = new TicketFixture(connection);
            var service = fixture.failingService();

            assertThrows(RuntimeException.class, () -> service.fileTicket(new NewTicketRequest(fixture.bookingId,
                    "Cleanliness", "Dirty", "Not cleaned", RemedyType.FULL_REFUND, null), fixture.ctx.guestId(),
                    Role.GUEST));

            assertTrue(fixture.tickets.findByRaisedByUserId(fixture.ctx.guestId()).isEmpty());
        }
    }

    @Test
    void failedAuditRollsBackCategoryCreation() throws Exception {
        try (Connection connection = migratedConnection()) {
            var fixture = new TicketFixture(connection);
            int before = fixture.categories.findAll().size();
            var service = fixture.failingService();

            assertThrows(RuntimeException.class, () -> service.createCategory("Noise", fixture.agent.userId()));

            assertEquals(before, fixture.categories.findAll().size());
        }
    }

    @Test
    void failedAuditRollsBackCategoryRename() throws Exception {
        try (Connection connection = migratedConnection()) {
            var fixture = new TicketFixture(connection);
            TicketCategory category = fixture.newCategory("Noise");
            var service = fixture.failingService();

            assertThrows(RuntimeException.class, () ->
                    service.renameCategory(category.categoryId(), "Loud", fixture.agent.userId()));

            assertEquals("Noise", fixture.categories.findById(category.categoryId()).orElseThrow().label());
        }
    }

    @Test
    void failedAuditRollsBackCategoryToggle() throws Exception {
        try (Connection connection = migratedConnection()) {
            var fixture = new TicketFixture(connection);
            TicketCategory category = fixture.newCategory("Noise");
            var service = fixture.failingService();

            assertThrows(RuntimeException.class, () ->
                    service.setCategoryActive(category.categoryId(), false, fixture.agent.userId()));

            assertTrue(fixture.categories.findById(category.categoryId()).orElseThrow().active());
        }
    }

    @Test
    void failedAuditRollsBackCategoryDelete() throws Exception {
        try (Connection connection = migratedConnection()) {
            var fixture = new TicketFixture(connection);
            TicketCategory category = fixture.newCategory("Noise");
            var service = fixture.failingService();

            assertThrows(RuntimeException.class, () ->
                    service.deleteCategory(category.categoryId(), fixture.agent.userId()));

            assertTrue(fixture.categories.findById(category.categoryId()).isPresent());
        }
    }

    @Test
    void failedAuditRollsBackAssignToMe() throws Exception {
        try (Connection connection = migratedConnection()) {
            var fixture = new TicketFixture(connection);
            Ticket ticket = fixture.openTicket();
            var service = fixture.failingService();

            assertThrows(RuntimeException.class, () -> service.assignToMe(ticket.ticketId(), fixture.agent.userId()));

            Ticket after = fixture.tickets.findById(ticket.ticketId()).orElseThrow();
            assertEquals(TicketStatus.OPEN, after.status());
            assertNull(after.assignedAgentId());
        }
    }

    @Test
    void failedAuditRollsBackSaveNotes() throws Exception {
        try (Connection connection = migratedConnection()) {
            var fixture = new TicketFixture(connection);
            Ticket ticket = fixture.openTicket();
            fixture.healthyService().assignToMe(ticket.ticketId(), fixture.agent.userId());
            String original = fixture.tickets.findById(ticket.ticketId()).orElseThrow().agentNotes();
            var service = fixture.failingService();

            assertThrows(RuntimeException.class, () ->
                    service.saveNotes(ticket.ticketId(), "new note", fixture.agent.userId()));

            assertEquals(original, fixture.tickets.findById(ticket.ticketId()).orElseThrow().agentNotes());
        }
    }

    @Test
    void failedAuditRollsBackUnassign() throws Exception {
        try (Connection connection = migratedConnection()) {
            var fixture = new TicketFixture(connection);
            Ticket ticket = fixture.openTicket();
            fixture.healthyService().assignToMe(ticket.ticketId(), fixture.agent.userId());
            var service = fixture.failingService();

            assertThrows(RuntimeException.class, () -> service.unassign(ticket.ticketId(), fixture.agent.userId()));

            Ticket after = fixture.tickets.findById(ticket.ticketId()).orElseThrow();
            assertEquals(TicketStatus.IN_REVIEW, after.status());
            assertEquals(fixture.agent.userId(), after.assignedAgentId());
        }
    }
}
