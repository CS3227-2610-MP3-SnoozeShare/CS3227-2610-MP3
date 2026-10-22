package com.snoozeshare.testsupport;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.ResolutionMode;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.service.TicketService;
import com.snoozeshare.service.TransactionService;
import com.snoozeshare.service.WalletService;
import com.snoozeshare.service.requests.NewTicketRequest;
import com.snoozeshare.service.requests.ResolutionRequest;

/** Creates reusable agent API scenarios with real service calls for money movements. */
public class DisputeScenarioFactory {

    private static final BigDecimal RATE = new BigDecimal("40.00");
    private static final BigDecimal BALANCE = new BigDecimal("1000.00");

    private final AgentTestUsers users;
    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final TicketRepository ticketsRepository;
    private final TicketService tickets;
    private final WalletService wallets;
    private final TransactionService transactions;
    private final JdbcTemplate jdbc;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DisputeScenarioFactory(AgentTestUsers users,
                                  PropertyRepository properties,
                                  BookingRepository bookings,
                                  TicketRepository ticketsRepository,
                                  TicketService tickets,
                                  WalletService wallets,
                                  TransactionService transactions,
                                  JdbcTemplate jdbc) {
        this.users = users;
        this.properties = properties;
        this.bookings = bookings;
        this.ticketsRepository = ticketsRepository;
        this.tickets = tickets;
        this.wallets = wallets;
        this.transactions = transactions;
        this.jdbc = jdbc;
    }

    public Scenario openDispute(RemedyType remedy) {
        return createDispute(remedy);
    }

    public Scenario inReviewDispute(UUID agentId) {
        Scenario scenario = openDispute(RemedyType.FULL_REFUND);
        Ticket assigned = tickets.assignToMe(scenario.ticket().getId(), agentId);
        return scenario.withTicket(assigned);
    }

    public Scenario resolvedDispute(UUID agentId) {
        Scenario scenario = inReviewDispute(agentId);
        Ticket resolved = tickets.resolve(scenario.ticket().getId(),
                new ResolutionRequest(ResolutionMode.REJECT, BigDecimal.ZERO, "Resolved"), agentId);
        return scenario.withTicket(resolved);
    }

    public Scenario releasedEscrowDispute() {
        Scenario scenario = openDispute(RemedyType.FULL_REFUND);
        transactions.settleBookingCompletion(scenario.booking().getId());
        return scenario;
    }

    public Scenario disputeInCategory(String label) {
        Scenario scenario = openDispute(RemedyType.FULL_REFUND);
        return scenario.withCategory(label);
    }

    public User guestWithConfirmedFutureBooking() {
        AgentTestUsers.TestUser guest = users.create(Role.GUEST);
        AgentTestUsers.TestUser host = users.create(Role.HOST);
        Property property = property(host.id());
        LocalDate start = LocalDate.now().plusDays(20);
        Booking booking = booking(property, guest.id(), start, BookingStatus.CONFIRMED);
        fundAndHold(guest.id(), booking);
        return guest.user();
    }

    public HostScenario hostWithTwoListingsAndOneBooking() {
        AgentTestUsers.TestUser host = users.create(Role.HOST);
        AgentTestUsers.TestUser guest = users.create(Role.GUEST);
        Property first = property(host.id());
        Property second = property(host.id());
        Booking booking = booking(first, guest.id(), LocalDate.now().plusDays(20),
                BookingStatus.CONFIRMED);
        fundAndHold(guest.id(), booking);
        return new HostScenario(host.user(), first, second, booking, guest.user());
    }

    public void bulk(int ticketCount, int accountCount, int auditRows) {
        AgentTestUsers.TestUser agent = users.create(Role.AGENT);
        AgentTestUsers.TestUser guest = users.create(Role.GUEST);
        AgentTestUsers.TestUser host = users.create(Role.HOST);
        Property property = property(host.id());
        Booking booking = booking(property, guest.id(), LocalDate.now().minusDays(4),
                BookingStatus.CONFIRMED);
        fundAndHold(guest.id(), booking);
        Instant now = Instant.now();
        for (int index = 0; index < ticketCount; index++) {
            Ticket ticket = new Ticket(UUID.randomUUID(), booking.getId(), guest.id(),
                    Role.GUEST, "Performance", "Ticket " + index, "Description",
                    RemedyType.OTHER, null, TicketStatus.OPEN, null, null, null, now, null);
            ticketsRepository.save(ticket);
        }
        for (int index = 0; index < accountCount; index++) {
            users.create(index % 2 == 0 ? Role.GUEST : Role.HOST);
        }
        for (int index = 0; index < auditRows; index++) {
            jdbc.update("insert into audit_log (id, actor_user_id, action_type, entity_type, "
                            + "entity_id, timestamp) values (?::uuid, ?::uuid, ?, ?, ?::uuid, ?)",
                    UUID.randomUUID().toString(), agent.id(), "TICKET_OPENED", "Ticket",
                    UUID.randomUUID().toString(), now);
        }
    }

    private Scenario createDispute(RemedyType remedy) {
        AgentTestUsers.TestUser agent = users.create(Role.AGENT);
        AgentTestUsers.TestUser guest = users.create(Role.GUEST);
        AgentTestUsers.TestUser host = users.create(Role.HOST);
        Property property = property(host.id());
        Booking booking = booking(property, guest.id(), LocalDate.now().minusDays(4),
                BookingStatus.CONFIRMED);
        fundAndHold(guest.id(), booking);
        String category = "Dispute " + UUID.randomUUID();
        tickets.createCategory(category, agent.id());
        Ticket ticket = tickets.fileTicket(new NewTicketRequest(booking.getId(), category,
                "Test dispute", "Test description", remedy, null), guest.id(), Role.GUEST);
        return new Scenario(agent.user(), guest.user(), host.user(), property, booking,
                ticket, category);
    }

    private Property property(UUID hostId) {
        Instant now = Instant.now();
        Property property = new Property(UUID.randomUUID(), hostId, ListingStatus.ACTIVE,
                "Test place " + UUID.randomUUID(), "Scenario property", PropertyType.APARTMENT,
                "1 Test Road", "Singapore", "Central", 100000, 2, 1, 1, RATE,
                LocalTime.of(14, 0), LocalTime.of(11, 0), Set.of(), now);
        return properties.save(property);
    }

    private Booking booking(Property property, UUID guestId, LocalDate start,
                           BookingStatus status) {
        Instant now = Instant.now();
        Booking booking = new Booking(UUID.randomUUID(), property.getId(), guestId,
                start, start.plusDays(3), status, RATE, RATE.multiply(BigDecimal.valueOf(3)),
                now, now, null, null);
        return bookings.save(booking);
    }

    private void fundAndHold(UUID guestId, Booking booking) {
        wallets.topUp(guestId, BALANCE);
        transactions.holdEscrow(booking.getId());
    }

    public record Scenario(User agent, User guest, User host, Property property,
                           Booking booking, Ticket ticket, String category) {
        public Scenario withTicket(Ticket replacement) {
            return new Scenario(agent, guest, host, property, booking, replacement, category);
        }

        public Scenario withCategory(String replacement) {
            return new Scenario(agent, guest, host, property, booking, ticket, replacement);
        }
    }

    public record HostScenario(User host, Property firstListing, Property secondListing,
                               Booking booking, User guest) { }
}
