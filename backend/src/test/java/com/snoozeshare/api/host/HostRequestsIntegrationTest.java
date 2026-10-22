package com.snoozeshare.api.host;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;

/** E3/E7/E26/E27: authenticated, owner-scoped Host booking request API. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(HostListingsControllerTest.TestAuthConfiguration.class)
class HostRequestsIntegrationTest {

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
    @Autowired private HostListingsControllerTest.FakeTokenVerifier verifier;

    private UUID hostId;
    private UUID guestId;
    private UUID propertyId;
    private String token;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        propertyId = UUID.randomUUID();
        token = "host-requests-" + UUID.randomUUID();
        verifier.add(token, token);
        authUsers.insert(new AuthUserRepository.AuthUser(hostId, token, token + "@example.com",
            AuthRole.HOST, "Host", "ACTIVE", null, null));
        users.save(new User(hostId, token, Role.HOST, "Host", token + "@example.com",
            AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
        users.save(new User(guestId, "guest-" + guestId, Role.GUEST, "Guest",
            "guest-" + guestId + "@example.com", AccountStatus.ACTIVE, null,
            Instant.now(), Instant.now(), null));
        properties.save(new Property(propertyId, hostId, ListingStatus.ACTIVE, "Host home",
            "Description", PropertyType.APARTMENT, "1 Main St", "Singapore", "Central", 123456,
            2, 1, 1, new BigDecimal("80.00"), LocalTime.of(15, 0), LocalTime.of(11, 0),
            Set.of(), Instant.now()));
    }

    @Test
    void pendingAndHistoryReturnAllOwnedRowsInOrderAndIgnoreLegacyPagination() throws Exception {
        Booking newestPending = null;
        Booking oldestPending = null;
        Booking newestHistory = null;
        Booking oldestHistory = null;
        Instant firstCreatedAt = Instant.parse("2026-02-01T00:00:00Z");
        for (int index = 0; index < 101; index++) {
            Booking pending = saveBooking(BookingStatus.PENDING, firstCreatedAt.plusSeconds(index));
            Booking history = saveBooking(BookingStatus.REJECTED, firstCreatedAt.plusSeconds(index));
            if (index == 0) {
                oldestPending = pending;
                oldestHistory = history;
            }
            if (index == 100) {
                newestPending = pending;
                newestHistory = history;
            }
        }
        UUID otherHost = UUID.randomUUID();
        UUID otherProperty = UUID.randomUUID();
        users.save(new User(otherHost, "other-" + otherHost, Role.HOST, "Other",
            "other-" + otherHost + "@example.com", AccountStatus.ACTIVE, null,
            Instant.now(), Instant.now(), null));
        properties.save(new Property(otherProperty, otherHost, ListingStatus.ACTIVE, "Other home",
            "Description", PropertyType.APARTMENT, "2 Main St", "Singapore", "Central", 123456,
            2, 1, 1, new BigDecimal("80.00"), LocalTime.of(15, 0), LocalTime.of(11, 0),
            Set.of(), Instant.now()));
        UUID foreignBooking = UUID.randomUUID();
        bookings.save(new Booking(foreignBooking, otherProperty, guestId,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(32), BookingStatus.PENDING,
            new BigDecimal("80.00"), new BigDecimal("160.00"), Instant.now(), null, null, null));

        String pendingResponse = mockMvc.perform(get("/api/host/requests?view=pending")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(101))
            .andExpect(jsonPath("$[0].booking.id").value(newestPending.getId().toString()))
            .andExpect(jsonPath("$[100].booking.id").value(oldestPending.getId().toString()))
            .andExpect(jsonPath("$[?(@.booking.id == '" + foreignBooking + "')]").isEmpty())
            .andExpect(jsonPath("$.page").doesNotExist())
            .andExpect(jsonPath("$.size").doesNotExist())
            .andExpect(jsonPath("$.hasMore").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        String pendingLegacyResponse = mockMvc.perform(get("/api/host/requests?view=pending&page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(101))
            .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(pendingLegacyResponse).isEqualTo(pendingResponse);

        String historyResponse = mockMvc.perform(get("/api/host/requests?view=history")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(101))
            .andExpect(jsonPath("$[0].booking.id").value(newestHistory.getId().toString()))
            .andExpect(jsonPath("$[100].booking.id").value(oldestHistory.getId().toString()))
            .andExpect(jsonPath("$[?(@.booking.id == '" + foreignBooking + "')]").isEmpty())
            .andReturn().getResponse().getContentAsString();
        String historyLegacyResponse = mockMvc.perform(get("/api/host/requests?view=history&page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(101))
            .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(historyLegacyResponse).isEqualTo(historyResponse);
        mockMvc.perform(get("/api/host/requests?view=invalid")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/host/requests/" + foreignBooking + "/decision")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"approve\":true}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void decisionRejectsOversizedUnicodeMessageAndRejectsARepeat() throws Exception {
        Booking booking = saveBooking(BookingStatus.PENDING, Instant.now());
        String oversized = "😀".repeat(501);
        mockMvc.perform(post("/api/host/requests/" + booking.getId() + "/decision")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approve\":true,\"message\":\"" + oversized + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/host/requests/" + booking.getId() + "/decision")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"approve\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mockMvc.perform(get("/api/host/requests?view=history")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].booking.id").value(booking.getId().toString()));
        mockMvc.perform(post("/api/host/requests/" + booking.getId() + "/decision")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"approve\":true}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("STATE_CONFLICT"));
    }

    private Booking saveBooking(BookingStatus state, Instant createdAt) {
        return bookings.save(new Booking(UUID.randomUUID(), propertyId, guestId,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(32), state,
            new BigDecimal("80.00"), new BigDecimal("160.00"), createdAt, null, null, null));
    }
}
