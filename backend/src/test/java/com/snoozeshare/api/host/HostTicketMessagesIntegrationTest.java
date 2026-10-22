package com.snoozeshare.api.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.infra.events.MessagePostedEvent;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.MessageRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.MessageService;

/** Host ticket-chat API keeps HOST and GUEST channels distinct and private. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(HostListingsControllerTest.TestAuthConfiguration.class)
@RecordApplicationEvents
@ExtendWith(OutputCaptureExtension.class)
class HostTicketMessagesIntegrationTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthUserRepository authUsers;
    @Autowired private PropertyRepository properties;
    @Autowired private UserRepository users;
    @Autowired private BookingRepository bookings;
    @Autowired private TicketRepository tickets;
    @Autowired private MessageRepository messageRows;
    @Autowired private MessageService messages;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEvents events;
    @Autowired private HostListingsControllerTest.FakeTokenVerifier verifier;

    private UUID hostId;
    private UUID ticketId;
    private String token;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        ticketId = UUID.randomUUID();
        token = "ticket-chat-" + UUID.randomUUID();
        verifier.add(token, token);
        authUsers.insert(new AuthUserRepository.AuthUser(hostId, token, token + "@example.com",
            AuthRole.HOST, "Host", "ACTIVE", null, null));
        users.save(new User(hostId, token, Role.HOST, "Host", token + "@example.com",
            AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
        users.save(new User(guestId, "guest-" + guestId, Role.GUEST, "Guest",
            "guest-" + guestId + "@example.com", AccountStatus.ACTIVE, null,
            Instant.now(), Instant.now(), null));
        properties.save(new Property(propertyId, hostId, ListingStatus.ACTIVE, "Ticket home",
            "Description", PropertyType.APARTMENT, "1 Main St", "Singapore", "Central", 123456,
            2, 1, 1, new BigDecimal("80.00"), LocalTime.of(15, 0), LocalTime.of(11, 0),
            Set.of(), Instant.now()));
        bookings.save(new Booking(bookingId, propertyId, guestId, LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(32), BookingStatus.CONFIRMED, new BigDecimal("80.00"),
            new BigDecimal("160.00"), Instant.now(), Instant.now(), null, null));
        tickets.save(new Ticket(ticketId, bookingId, guestId, Role.GUEST, "Other", "Help",
            "Ticket description", RemedyType.OTHER, null, TicketStatus.OPEN, null, null,
            null, Instant.now(), null));
    }

    @Test
    void hostGetsCompleteTicketCollectionsAndWrongChannelOrForeignHostIsHidden(CapturedOutput output) throws Exception {
        sendMessage("first support note");
        sendMessage("second support note");
        Ticket baseTicket = tickets.findById(ticketId).orElseThrow();
        for (int index = 0; index < 100; index++) {
            UUID id = UUID.randomUUID();
            tickets.save(new Ticket(id, baseTicket.getBookingId(), baseTicket.getRaisedByUserId(),
                Role.GUEST, "Other", "Issue " + index, "Description", RemedyType.OTHER, null,
                TicketStatus.OPEN, null, null, null, Instant.now().minusSeconds(index + 1L), null));
        }
        String summaries = mockMvc.perform(get("/api/host/messages/tickets")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(101))
            .andExpect(jsonPath("$[0].ticketId").value(ticketId.toString()))
            .andExpect(jsonPath("$[0].channel").value("HOST"))
            .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/host/messages/tickets?page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(result ->
                org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                    .isEqualTo(summaries));
        org.assertj.core.api.Assertions.assertThat(output)
            .doesNotContain("first support note", "second support note");
        for (int index = 0; index < 101; index++) {
            messageRows.save(new Message(UUID.randomUUID(), ticketId, ThreadChannel.HOST,
                baseTicket.getRaisedByUserId(), Role.GUEST, "bulk-" + index,
                Instant.parse("2027-01-01T00:00:00Z").plusSeconds(index)));
        }
        String history = mockMvc.perform(get("/api/host/messages/tickets/" + ticketId + "?channel=HOST")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(103))
            .andExpect(jsonPath("$[0].body").value("bulk-100"))
            .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/host/messages/tickets/" + ticketId + "?channel=HOST&page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(result ->
                org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                    .isEqualTo(history));
        mockMvc.perform(get("/api/host/messages/tickets/" + ticketId + "?channel=GUEST")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());

        UUID foreignHost = UUID.randomUUID();
        String foreignToken = "foreign-ticket-" + foreignHost;
        verifier.add(foreignToken, foreignToken);
        authUsers.insert(new AuthUserRepository.AuthUser(foreignHost, foreignToken,
            foreignToken + "@example.com", AuthRole.HOST, "Other", "ACTIVE", null, null));
        users.save(new User(foreignHost, foreignToken, Role.HOST, "Other",
            foreignToken + "@example.com", AccountStatus.ACTIVE, null,
            Instant.now(), Instant.now(), null));
        mockMvc.perform(get("/api/host/messages/tickets/" + ticketId + "?channel=HOST")
                .header("Authorization", "Bearer " + foreignToken))
            .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/host/messages/tickets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void resolvedTicketIsReadOnly() throws Exception {
        Ticket ticket = tickets.findById(ticketId).orElseThrow();
        ticket.setStatus(TicketStatus.RESOLVED_REJECTED);
        ticket.setResolvedAt(Instant.now());
        tickets.save(ticket);
        mockMvc.perform(post("/api/host/messages/tickets/" + ticketId + "?channel=HOST")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"cannot post\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    @Test
    void ticketMessageRollbackPublishesNoEvent() {
        long before = events.stream(MessagePostedEvent.class).count();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
                messages.post(ticketId, ThreadChannel.HOST, hostId, Role.HOST,
                    "rolled back ticket content");
                throw new IllegalStateException("test rollback");
            });
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("test rollback");
        }
        assertThat(messageRows.findThread(ticketId, ThreadChannel.HOST)).isEmpty();
        assertThat(events.stream(MessagePostedEvent.class).count()).isEqualTo(before);
    }

    private void sendMessage(String body) throws Exception {
        mockMvc.perform(post("/api/host/messages/tickets/" + ticketId + "?channel=HOST")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"))
            .andExpect(status().isCreated());
    }
}
