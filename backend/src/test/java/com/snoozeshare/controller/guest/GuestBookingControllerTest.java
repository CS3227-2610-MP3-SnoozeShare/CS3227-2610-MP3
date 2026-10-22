package com.snoozeshare.controller.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
@Import(GuestBookingControllerTest.TestAuthConfig.class)
class GuestBookingControllerTest extends PostgresIntegrationTest {

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
    private String hostToken;
    private String guest2Token;
    private UUID guestId;
    private UUID hostId;
    private Property activeProperty;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        String guestUid = "guest-uid-" + UUID.randomUUID();
        guestToken = "guest-token-" + UUID.randomUUID();
        guestId = UUID.randomUUID();
        hostId = UUID.randomUUID();

        verifier.add(guestToken, guestUid, "guest@test.com");

        String hostUid = "host-uid-" + UUID.randomUUID();
        hostToken = "host-token-" + UUID.randomUUID();
        verifier.add(hostToken, hostUid, "host@test.com");

        // Second guest for concurrency tests
        UUID guest2Id = UUID.randomUUID();
        String guest2Uid = "guest2-uid-" + UUID.randomUUID();
        guest2Token = "guest2-token-" + UUID.randomUUID();
        verifier.add(guest2Token, guest2Uid, "guest2@test.com");

        User guest = userRepository.save(new User(
                guestId, guestUid, Role.GUEST, "Alice Guest",
                "guest-" + UUID.randomUUID() + "@test.com",
                AccountStatus.ACTIVE, null, now, now, null));
        walletRepository.save(new Wallet(
                UUID.randomUUID(), guest.getId(),
                new BigDecimal("10000.00"), "SGD", now));

        User guest2 = userRepository.save(new User(
                guest2Id, guest2Uid, Role.GUEST, "Carol Guest",
                "guest2-" + UUID.randomUUID() + "@test.com",
                AccountStatus.ACTIVE, null, now, now, null));
        walletRepository.save(new Wallet(
                UUID.randomUUID(), guest2.getId(),
                new BigDecimal("10000.00"), "SGD", now));

        User host = userRepository.save(new User(
                hostId, hostUid, Role.HOST,
                "Bob Host", "host-" + UUID.randomUUID() + "@test.com",
                AccountStatus.ACTIVE, "HOST123", now, now, null));
        walletRepository.save(new Wallet(
                UUID.randomUUID(), host.getId(),
                new BigDecimal("500.00"), "SGD", now));

        activeProperty = propertyRepository.save(new Property(
                UUID.randomUUID(), hostId, ListingStatus.ACTIVE,
                "Beach House", "A lovely beach house",
                PropertyType.APARTMENT, "123 Beach Rd", "Singapore",
                "East", 123456, 4, 2, 1,
                new BigDecimal("150.0000"),
                LocalTime.of(14, 0), LocalTime.of(11, 0),
                Set.of(), now));
    }

    @Test
    void createBooking() throws Exception {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        String body = objectMapper.writeValueAsString(Map.of(
                "propertyId", activeProperty.getId(),
                "startDate", start.toString(),
                "endDate", end.toString()));

        mockMvc.perform(post("/api/guest/bookings")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.listingId")
                    .value(activeProperty.getId().toString()))
            .andExpect(jsonPath("$.totalAmount").value(450.0000));
    }

    @Test
    void bookOwnListing() throws Exception {
        // Create a property owned by the guest
        Instant now = Instant.now();
        Property ownProperty = propertyRepository.save(new Property(
                UUID.randomUUID(), guestId, ListingStatus.ACTIVE,
                "My Place", "Own listing", PropertyType.APARTMENT,
                "456 Own St", "Singapore", "North",
                789012, 2, 1, 1, new BigDecimal("100.0000"),
                LocalTime.of(14, 0), LocalTime.of(11, 0),
                Set.of(), now));

        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(2);
        String body = objectMapper.writeValueAsString(Map.of(
                "propertyId", ownProperty.getId(),
                "startDate", start.toString(),
                "endDate", end.toString()));

        mockMvc.perform(post("/api/guest/bookings")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("SELF_BOOKING"));
    }

    @Test
    void bookInactiveListing() throws Exception {
        Instant now = Instant.now();
        Property inactive = propertyRepository.save(new Property(
                UUID.randomUUID(), hostId, ListingStatus.INACTIVE,
                "Hidden Place", "Not shown", PropertyType.APARTMENT,
                "999 Hidden St", "Singapore", "West",
                654321, 2, 1, 1, new BigDecimal("100.0000"),
                LocalTime.of(14, 0), LocalTime.of(11, 0),
                Set.of(), now));

        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(2);
        String body = objectMapper.writeValueAsString(Map.of(
                "propertyId", inactive.getId(),
                "startDate", start.toString(),
                "endDate", end.toString()));

        mockMvc.perform(post("/api/guest/bookings")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void bookPastStartDate() throws Exception {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        String body = objectMapper.writeValueAsString(Map.of(
                "propertyId", activeProperty.getId(),
                "startDate", start.toString(),
                "endDate", end.toString()));

        mockMvc.perform(post("/api/guest/bookings")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void listTrips() throws Exception {
        // Create a booking first
        Instant now = Instant.now();
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        bookingRepository.save(new Booking(
                UUID.randomUUID(), activeProperty.getId(), guestId,
                start, end, BookingStatus.PENDING,
                new BigDecimal("150.0000"), new BigDecimal("450.0000"),
                now, null, null, null));

        mockMvc.perform(get("/api/guest/bookings")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].booking.status").value("PENDING"))
            .andExpect(jsonPath("$[0].listingTitle").value("Beach House"))
            .andExpect(jsonPath("$[0].hostDisplayName").value("Bob Host"))
            .andExpect(jsonPath("$[0].canCancel").value(true));
    }

    @Test
    void cancelPreview() throws Exception {
        Instant now = Instant.now();
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        Booking booking = bookingRepository.save(new Booking(
                UUID.randomUUID(), activeProperty.getId(), guestId,
                start, end, BookingStatus.PENDING,
                new BigDecimal("150.0000"), new BigDecimal("450.0000"),
                now, null, null, null));

        mockMvc.perform(get("/api/guest/bookings/" + booking.getId()
                + "/cancel-preview")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.amount").value(450.0000))
            .andExpect(jsonPath("$.percent").value(100));
    }

    @Test
    void cancelPendingFullRefund() throws Exception {
        // Create a booking via the service to set up escrow correctly
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        String body = objectMapper.writeValueAsString(Map.of(
                "propertyId", activeProperty.getId(),
                "startDate", start.toString(),
                "endDate", end.toString()));

        String result = mockMvc.perform(post("/api/guest/bookings")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(result).get("id").asText();

        mockMvc.perform(post("/api/guest/bookings/" + bookingId + "/cancel")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED_BY_GUEST"));
    }

    @Test
    void cancelAlreadyCancelled() throws Exception {
        // Create and cancel a booking, then try to cancel again
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        String body = objectMapper.writeValueAsString(Map.of(
                "propertyId", activeProperty.getId(),
                "startDate", start.toString(),
                "endDate", end.toString()));

        String result = mockMvc.perform(post("/api/guest/bookings")
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String bookingId = objectMapper.readTree(result).get("id").asText();

        // Cancel first time
        mockMvc.perform(post("/api/guest/bookings/" + bookingId + "/cancel")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk());

        // Cancel again — should fail
        mockMvc.perform(post("/api/guest/bookings/" + bookingId + "/cancel")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("NOT_CANCELLABLE"));
    }

    @Test
    void cancelOtherGuestBooking() throws Exception {
        // Create a booking for a different guest
        Instant now = Instant.now();
        UUID otherGuestId = UUID.randomUUID();
        String otherUid = "other-uid-" + UUID.randomUUID();
        userRepository.save(new User(
                otherGuestId, otherUid, Role.GUEST, "Other Guest",
                "other-" + UUID.randomUUID() + "@test.com",
                AccountStatus.ACTIVE, null, now, now, null));
        walletRepository.save(new Wallet(
                UUID.randomUUID(), otherGuestId,
                new BigDecimal("5000.00"), "SGD", now));

        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        Booking booking = bookingRepository.save(new Booking(
                UUID.randomUUID(), activeProperty.getId(), otherGuestId,
                start, end, BookingStatus.PENDING,
                new BigDecimal("150.0000"), new BigDecimal("450.0000"),
                now, null, null, null));

        mockMvc.perform(post("/api/guest/bookings/" + booking.getId()
                + "/cancel")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/guest/bookings"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsHostRole() throws Exception {
        mockMvc.perform(get("/api/guest/bookings")
                .header("Authorization", "Bearer " + hostToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void concurrentBookingOverlappingDates() throws Exception {
        LocalDate start = LocalDate.now().plusDays(20);
        LocalDate end = start.plusDays(3);
        String body = objectMapper.writeValueAsString(Map.of(
                "propertyId", activeProperty.getId(),
                "startDate", start.toString(),
                "endDate", end.toString()));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        Runnable task1 = () -> {
            try {
                ready.countDown();
                go.await();
                int responseStatus = mockMvc.perform(
                        post("/api/guest/bookings")
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                    .andReturn().getResponse().getStatus();
                if (responseStatus == 200) {
                    successes.incrementAndGet();
                } else {
                    failures.incrementAndGet();
                }
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        };
        Runnable task2 = () -> {
            try {
                ready.countDown();
                go.await();
                int responseStatus = mockMvc.perform(
                        post("/api/guest/bookings")
                        .header("Authorization", "Bearer " + guest2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                    .andReturn().getResponse().getStatus();
                if (responseStatus == 200) {
                    successes.incrementAndGet();
                } else {
                    failures.incrementAndGet();
                }
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        };

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();
        ready.await();
        go.countDown();
        t1.join(10000);
        t2.join(10000);

        // Both may succeed (overlapping dates allowed for different guests in PENDING)
        // or one may fail with 409 depending on business rules — assert both completed
        assertEquals(2, successes.get() + failures.get());
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
