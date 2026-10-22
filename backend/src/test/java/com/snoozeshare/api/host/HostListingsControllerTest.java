package com.snoozeshare.api.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
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
import com.snoozeshare.domain.model.Review;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.ReviewRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.ListingMetricsService;

/** Exercises authenticated Host listing contracts against the real service and PostgreSQL repositories. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(HostListingsControllerTest.TestAuthConfiguration.class)
class HostListingsControllerTest {

    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthUserRepository authUsers;

    @Autowired
    private PropertyRepository properties;

    @Autowired
    private UserRepository users;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private ReviewRepository reviews;

    @MockitoSpyBean
    private ListingMetricsService listingMetrics;

    @Autowired
    private FakeTokenVerifier verifier;

    private UUID hostId;
    private String token;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        token = "host-listings-" + UUID.randomUUID();
        verifier.add(token, token);
        authUsers.insert(new AuthUserRepository.AuthUser(hostId, token, token + "@example.com",
            AuthRole.HOST, "Host", "ACTIVE", null, null));
        users.save(new User(hostId, token, Role.HOST, "Host", token + "@example.com",
            AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
    }

    @Test
    void listingCollectionReturnsAllOwnedPropertiesAndIgnoresLegacyPagination() throws Exception {
        Property oldest = null;
        Property newest = null;
        for (int index = 0; index < 101; index++) {
            Property property = saveProperty(hostId, "Listing " + index,
                Instant.parse("2026-01-01T00:00:00Z").plusSeconds(index));
            if (index == 0) {
                oldest = property;
            }
            if (index == 100) {
                newest = property;
            }
        }
        UUID otherHostId = UUID.randomUUID();
        saveUser(otherHostId, "other-host-" + otherHostId);
        Property foreign = saveProperty(otherHostId, "Other host",
            Instant.parse("2026-01-03T00:00:00Z"));
        UUID guestId = UUID.randomUUID();
        saveGuest(guestId);
        saveReviewedBooking(newest, guestId, 4);
        saveReviewedBooking(newest, guestId, 5);
        saveReviewedBooking(foreign, guestId, 1);

        String completeResponse = mockMvc.perform(get("/api/host/listings")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(101))
            .andExpect(jsonPath("$[0].id").value(newest.getId().toString()))
            .andExpect(jsonPath("$[0].hostId").value(hostId.toString()))
            .andExpect(jsonPath("$[0].bookingCount").value(2))
            .andExpect(jsonPath("$[0].averageRating").value(4.5))
            .andExpect(jsonPath("$[100].id").value(oldest.getId().toString()))
            .andExpect(jsonPath("$[100].bookingCount").value(0))
            .andExpect(jsonPath("$[100].averageRating").value(0.0))
            .andExpect(jsonPath("$[?(@.id == '" + foreign.getId() + "')]").isEmpty())
            .andExpect(jsonPath("$.page").doesNotExist())
            .andExpect(jsonPath("$.size").doesNotExist())
            .andExpect(jsonPath("$.hasMore").doesNotExist())
            .andReturn().getResponse().getContentAsString();

        String legacyResponse = mockMvc.perform(get("/api/host/listings?page=0&size=1")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(101))
            .andExpect(jsonPath("$[0].id").value(newest.getId().toString()))
            .andReturn().getResponse().getContentAsString();
        assertThat(legacyResponse).isEqualTo(completeResponse);
    }

    @Test
    void emptyListingCollectionSkipsTheSummaryMetricsService() throws Exception {
        mockMvc.perform(get("/api/host/listings")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        verify(listingMetrics, never()).summaryMetricsForHost(hostId);
    }

    @Test
    void detailCombinesOwnedListingWithMetrics() throws Exception {
        Property property = saveProperty(hostId, "Detail listing");

        mockMvc.perform(get("/api/host/listings/" + property.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.listing.id").value(property.getId().toString()))
            .andExpect(jsonPath("$.metrics.bookingCount").value(0))
            .andExpect(jsonPath("$.metrics.reviews").isArray());
    }

    @Test
    void foreignListingCannotBeReadUpdatedOrStatusChanged() throws Exception {
        UUID otherHostId = UUID.randomUUID();
        saveUser(otherHostId, "other-host-" + otherHostId);
        Property foreign = saveProperty(otherHostId, "Foreign listing");

        mockMvc.perform(get("/api/host/listings/" + foreign.getId())
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(put("/api/host/listings/" + foreign.getId())
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(updateBody("Changed")))
            .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/host/listings/" + foreign.getId() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"status\":\"INACTIVE\"}"))
            .andExpect(status().isNotFound());
        assertThat(properties.findById(foreign.getId()).orElseThrow().getStatus()).isEqualTo(ListingStatus.ACTIVE);
    }

    @Test
    void createUpdateAndStatusChangesUseTheServerOwnedHostId() throws Exception {
        String createBody = "{\"title\":\"Created\",\"description\":\"A safe place\","
            + "\"propertyType\":\"APARTMENT\",\"status\":\"ACTIVE\","
            + "\"streetAddress\":\"1 Main St\",\"city\":\"Singapore\",\"region\":\"Central\","
            + "\"postalCode\":123456,\"maxGuests\":2,\"bedrooms\":1,\"bathrooms\":1,"
            + "\"baseNightlyRate\":80.00,\"checkInTime\":\"15:00:00\","
            + "\"checkOutTime\":\"11:00:00\",\"amenities\":[]}";
        String response = mockMvc.perform(post("/api/host/listings")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(createBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.hostId").value(hostId.toString()))
            .andReturn().getResponse().getContentAsString();
        UUID createdId = UUID.fromString(com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(response).get("id").asText());

        mockMvc.perform(put("/api/host/listings/" + createdId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(updateBody("Updated")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated"))
            .andExpect(jsonPath("$.hostId").value(hostId.toString()));
        mockMvc.perform(put("/api/host/listings/" + createdId + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"status\":\"INACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void rejectsForgedHostIdAndInvalidListingLengths() throws Exception {
        String forged = "{\"title\":\"Created\",\"description\":\"A safe place\","
            + "\"propertyType\":\"APARTMENT\",\"status\":\"ACTIVE\","
            + "\"streetAddress\":\"1 Main St\",\"city\":\"Singapore\",\"region\":\"Central\","
            + "\"postalCode\":123456,\"maxGuests\":2,\"bedrooms\":1,\"bathrooms\":1,"
            + "\"baseNightlyRate\":80.00,\"checkInTime\":\"15:00:00\","
            + "\"checkOutTime\":\"11:00:00\",\"amenities\":[],\"hostId\":\""
            + UUID.randomUUID() + "\"}";
        mockMvc.perform(post("/api/host/listings")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(forged))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/host/listings")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(createBody("T".repeat(121), "valid description")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/host/listings")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(createBody("Valid title", "D".repeat(5001))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private Property saveProperty(UUID ownerId, String title) {
        return saveProperty(ownerId, title, Instant.now());
    }

    private Property saveProperty(UUID ownerId, String title, Instant createdAt) {
        return properties.save(new Property(UUID.randomUUID(), ownerId, ListingStatus.ACTIVE, title,
            "Description", PropertyType.APARTMENT, "1 Main St", "Singapore", "Central", 123456,
            2, 1, 1, new BigDecimal("80.00"), LocalTime.of(15, 0), LocalTime.of(11, 0),
            Set.of(), createdAt));
    }

    private void saveUser(UUID id, String uid) {
        users.save(new User(id, uid, Role.HOST, "Other", uid + "@example.com", AccountStatus.ACTIVE,
            null, Instant.now(), Instant.now(), null));
    }

    private void saveGuest(UUID id) {
        String uid = "guest-" + id;
        users.save(new User(id, uid, Role.GUEST, "Guest", uid + "@example.com",
            AccountStatus.ACTIVE, null, Instant.now(), Instant.now(), null));
    }

    private void saveReviewedBooking(Property property, UUID guestId, int rating) {
        UUID bookingId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        bookings.save(new Booking(bookingId, property.getId(), guestId,
            LocalDate.now().plusDays(30), LocalDate.now().plusDays(32), BookingStatus.CONFIRMED,
            new BigDecimal("80.00"), new BigDecimal("160.00"), createdAt, null, null, null));
        reviews.save(new Review(UUID.randomUUID(), bookingId, guestId, rating,
            "Useful review", createdAt));
    }

    private String createBody(String title, String description) {
        return "{\"title\":\"" + title + "\",\"description\":\"" + description + "\","
            + "\"propertyType\":\"APARTMENT\",\"status\":\"ACTIVE\","
            + "\"streetAddress\":\"1 Main St\",\"city\":\"Singapore\",\"region\":\"Central\","
            + "\"postalCode\":123456,\"maxGuests\":2,\"bedrooms\":1,\"bathrooms\":1,"
            + "\"baseNightlyRate\":80.00,\"checkInTime\":\"15:00:00\","
            + "\"checkOutTime\":\"11:00:00\",\"amenities\":[]}";
    }

    private String updateBody(String title) {
        return "{\"title\":\"" + title + "\",\"description\":\"Updated description\","
            + "\"propertyType\":\"APARTMENT\",\"streetAddress\":\"1 Main St\","
            + "\"city\":\"Singapore\",\"region\":\"Central\",\"postalCode\":123456,"
            + "\"maxGuests\":2,\"bedrooms\":1,\"bathrooms\":1,\"baseNightlyRate\":90.00,"
            + "\"checkInTime\":\"15:00:00\",\"checkOutTime\":\"11:00:00\",\"amenities\":[]}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestAuthConfiguration {

        @Bean
        @Primary
        FakeTokenVerifier hostListingsTokenVerifier() {
            return new FakeTokenVerifier();
        }

        @Bean
        @Primary
        AuthUserRepository hostListingsAuthUsers() {
            return new InMemoryAuthUserRepository();
        }
    }

    static class FakeTokenVerifier implements FirebaseTokenVerifier {

        private final Map<String, FirebaseAuthentication> tokens = new ConcurrentHashMap<>();

        void add(String token, String uid) {
            tokens.put(token, new FirebaseAuthentication(uid, uid + "@example.com", true));
        }

        @Override
        public FirebaseAuthentication verify(String idToken) throws VerificationException {
            FirebaseAuthentication identity = tokens.get(idToken);
            if (identity == null) {
                throw new VerificationException(VerificationFailure.INVALID_TOKEN, null);
            }
            return identity;
        }
    }

    static class InMemoryAuthUserRepository implements AuthUserRepository {

        private final Map<String, AuthUser> byUid = new ConcurrentHashMap<>();

        @Override
        public Optional<AuthUser> findByFirebaseUid(String firebaseUid) {
            return Optional.ofNullable(byUid.get(firebaseUid));
        }

        @Override
        public Optional<AuthUser> findById(UUID id) {
            return byUid.values().stream().filter(user -> user.id().equals(id)).findFirst();
        }

        @Override
        public AuthUser insert(AuthUser user) {
            byUid.put(user.firebaseUid(), user);
            return user;
        }
    }
}
