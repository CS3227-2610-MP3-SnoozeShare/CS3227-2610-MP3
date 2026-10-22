package com.snoozeshare.controller.guest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.PostgresIntegrationTest;
import com.snoozeshare.auth.security.FirebaseTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(GuestReviewControllerTest.TestAuthConfig.class)
class GuestReviewControllerTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FakeTokenVerifier verifier;

    @Autowired
    private ObjectMapper objectMapper;

    private String guestToken;
    private UUID guestId;
    private Booking completedBooking;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        String guestUid = "guest-uid-" + UUID.randomUUID();
        guestToken = "guest-token-" + UUID.randomUUID();
        guestId = UUID.randomUUID();
        UUID hostId = UUID.randomUUID();

        verifier.add(guestToken, guestUid, "guest@test.com");

        User guest = userRepository.save(new User(
                guestId, guestUid, Role.GUEST, "Alice Guest",
                "guest-" + UUID.randomUUID() + "@test.com",
                AccountStatus.ACTIVE, null, now, now, null));
        walletRepository.save(new Wallet(
                UUID.randomUUID(), guest.getId(),
                new BigDecimal("1000.00"), "SGD", now));

        User host = userRepository.save(new User(
                hostId, "host-uid-" + UUID.randomUUID(), Role.HOST,
                "Bob Host", "host-" + UUID.randomUUID() + "@test.com",
                AccountStatus.ACTIVE, "HOST123", now, now, null));
        walletRepository.save(new Wallet(
                UUID.randomUUID(), host.getId(),
                new BigDecimal("500.00"), "SGD", now));

        Property property = propertyRepository.save(new Property(
                UUID.randomUUID(), hostId, ListingStatus.ACTIVE,
                "Beach House", "A lovely beach house",
                PropertyType.APARTMENT, "123 Beach Rd", "Singapore",
                "East", 123456, 4, 2, 1,
                new BigDecimal("150.0000"),
                LocalTime.of(14, 0), LocalTime.of(11, 0),
                Set.of(), now));

        completedBooking = bookingRepository.save(new Booking(
                UUID.randomUUID(), property.getId(), guestId,
                LocalDate.now().minusDays(10),
                LocalDate.now().minusDays(7),
                BookingStatus.COMPLETED,
                new BigDecimal("150.0000"), new BigDecimal("450.0000"),
                now, now, now, null));
    }

    @Test
    void submitReview() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("rating", 4, "comment", "Great stay!"));

        mockMvc.perform(post("/api/guest/bookings/" + completedBooking.getId()
                + "/review")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rating").value(4))
            .andExpect(jsonPath("$.comment").value("Great stay!"));
    }

    @Test
    void reviewInvalidRating() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("rating", 0, "comment", "Bad"));

        mockMvc.perform(post("/api/guest/bookings/" + completedBooking.getId()
                + "/review")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void reviewBlankComment() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("rating", 3, "comment", ""));

        mockMvc.perform(post("/api/guest/bookings/" + completedBooking.getId()
                + "/review")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void reviewPendingBooking() throws Exception {
        Instant now = Instant.now();
        Booking pending = bookingRepository.save(new Booking(
                UUID.randomUUID(), completedBooking.getListingId(), guestId,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(13),
                BookingStatus.PENDING,
                new BigDecimal("150.0000"), new BigDecimal("450.0000"),
                now, null, null, null));

        String body = objectMapper.writeValueAsString(
                Map.of("rating", 5, "comment", "Premature review"));

        mockMvc.perform(post("/api/guest/bookings/" + pending.getId()
                + "/review")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("NOT_REVIEWABLE"));
    }

    @Test
    void reviewDuplicate() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("rating", 4, "comment", "Great stay!"));

        // First review succeeds
        mockMvc.perform(post("/api/guest/bookings/" + completedBooking.getId()
                + "/review")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());

        // Second review fails
        mockMvc.perform(post("/api/guest/bookings/" + completedBooking.getId()
                + "/review")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("DUPLICATE_REVIEW"));
    }

    @Test
    void reviewCommentTooLong() throws Exception {
        String longComment = "x".repeat(2001);
        String body = objectMapper.writeValueAsString(
                Map.of("rating", 4, "comment", longComment));

        mockMvc.perform(post("/api/guest/bookings/" + completedBooking.getId()
                + "/review")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsUnauthenticated() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("rating", 4, "comment", "test"));

        mockMvc.perform(post("/api/guest/bookings/" + completedBooking.getId()
                + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAuthConfig {

        @Bean
        @Primary
        FakeTokenVerifier fakeTokenVerifier() {
            return new FakeTokenVerifier();
        }
    }

    static class FakeTokenVerifier implements FirebaseTokenVerifier {

        private final Map<String, FirebaseAuthentication> tokens =
                new ConcurrentHashMap<>();

        void add(String token, String uid, String email) {
            tokens.put(token, new FirebaseAuthentication(uid, email, true));
        }

        @Override
        public FirebaseAuthentication verify(String idToken)
                throws VerificationException {
            FirebaseAuthentication auth = tokens.get(idToken);
            if (auth == null) {
                throw new VerificationException(
                        VerificationFailure.INVALID_TOKEN, null);
            }
            return auth;
        }
    }
}
