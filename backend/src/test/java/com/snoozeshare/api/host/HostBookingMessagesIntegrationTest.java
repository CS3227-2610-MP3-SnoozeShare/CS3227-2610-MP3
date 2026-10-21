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
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.infra.events.BookingMessagePostedEvent;
import com.snoozeshare.repository.BookingMessageRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.BookingConversationService;

/** Host booking-chat API: privacy, bounded reads, and after-commit delivery. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(HostListingsControllerTest.TestAuthConfiguration.class)
@RecordApplicationEvents
@ExtendWith(OutputCaptureExtension.class)
class HostBookingMessagesIntegrationTest {

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
    @Autowired private BookingMessageRepository messages;
    @Autowired private HostListingsControllerTest.FakeTokenVerifier verifier;
    @Autowired private BookingConversationService conversations;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ApplicationEvents events;

    private UUID hostId;
    private UUID guestId;
    private UUID propertyId;
    private UUID bookingId;
    private String token;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        propertyId = UUID.randomUUID();
        bookingId = UUID.randomUUID();
        token = "booking-chat-" + UUID.randomUUID();
        verifier.add(token, token);
        authUsers.insert(new AuthUserRepository.AuthUser(hostId, token, token + "@example.com",
            AuthRole.HOST, "Host", "ACTIVE", null, null));
        users.save(new User(hostId, token, Role.HOST, "Host", token + "@example.com",
            AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
        users.save(new User(guestId, "guest-" + guestId, Role.GUEST, "Guest",
            "guest-" + guestId + "@example.com", AccountStatus.ACTIVE, null,
            Instant.now(), Instant.now(), null));
        properties.save(new Property(propertyId, hostId, ListingStatus.ACTIVE, "Chat home",
            "Description", PropertyType.APARTMENT, "1 Main St", "Singapore", "Central", 123456,
            2, 1, 1, new BigDecimal("80.00"), LocalTime.of(15, 0), LocalTime.of(11, 0),
            Set.of(), Instant.now()));
        bookings.save(new Booking(bookingId, propertyId, guestId, LocalDate.now().plusDays(30),
            LocalDate.now().plusDays(32), BookingStatus.CONFIRMED, new BigDecimal("80.00"),
            new BigDecimal("160.00"), Instant.now(), Instant.now(), null, null));
    }

    @Test
    void hostCanPagePostAndReadBookingChatWithoutExposingItToAnotherHost() throws Exception {
        sendMessage("first private note");
        sendMessage("second private note");
        UUID secondBooking = UUID.randomUUID();
        bookings.save(new Booking(secondBooking, propertyId, guestId, LocalDate.now().plusDays(40),
            LocalDate.now().plusDays(42), BookingStatus.CONFIRMED, new BigDecimal("80.00"),
            new BigDecimal("160.00"), Instant.now().minusSeconds(30), Instant.now(), null, null));

        mockMvc.perform(get("/api/host/messages/bookings?page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].bookingId").value(bookingId.toString()))
            .andExpect(jsonPath("$.hasMore").value(true));
        mockMvc.perform(get("/api/host/messages/bookings/" + bookingId + "?page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].body").value("second private note"))
            .andExpect(jsonPath("$.hasMore").value(true));
        mockMvc.perform(post("/api/host/messages/bookings/" + bookingId + "/read")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());

        UUID foreignHost = UUID.randomUUID();
        String foreignToken = "foreign-chat-" + foreignHost;
        verifier.add(foreignToken, foreignToken);
        authUsers.insert(new AuthUserRepository.AuthUser(foreignHost, foreignToken,
            foreignToken + "@example.com", AuthRole.HOST, "Other", "ACTIVE", null, null));
        users.save(new User(foreignHost, foreignToken, Role.HOST, "Other",
            foreignToken + "@example.com", AccountStatus.ACTIVE, null,
            Instant.now(), Instant.now(), null));
        mockMvc.perform(get("/api/host/messages/bookings/" + bookingId)
                .header("Authorization", "Bearer " + foreignToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void messageEventsWaitForCommitAndRollbackPublishesNothing(CapturedOutput output) throws Exception {
        mockMvc.perform(post("/api/host/messages/bookings/" + bookingId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"authorized private content\"}"))
            .andExpect(status().isCreated());
        assertThat(events.stream(BookingMessagePostedEvent.class)
            .filter(event -> bookingId.equals(event.bookingId()))).hasSize(1);
        assertThat(output).doesNotContain("authorized private content");

        long before = events.stream(BookingMessagePostedEvent.class).count();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(ignored -> {
                conversations.post(bookingId, hostId, Role.HOST, "rolled back content");
                throw new IllegalStateException("test rollback");
            });
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessage("test rollback");
        }
        assertThat(messages.findThread(bookingId)).hasSize(1);
        assertThat(events.stream(BookingMessagePostedEvent.class).count()).isEqualTo(before);
    }

    private void sendMessage(String body) throws Exception {
        mockMvc.perform(post("/api/host/messages/bookings/" + bookingId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + body + "\"}"))
            .andExpect(status().isCreated());
    }
}
