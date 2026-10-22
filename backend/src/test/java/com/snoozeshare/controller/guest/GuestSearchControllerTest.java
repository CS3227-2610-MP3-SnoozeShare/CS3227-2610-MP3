package com.snoozeshare.controller.guest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

import com.snoozeshare.PostgresIntegrationTest;
import com.snoozeshare.auth.security.FirebaseTokenVerifier;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "snoozeshare.security.enabled=true")
@AutoConfigureMockMvc
@Import(GuestSearchControllerTest.TestAuthConfig.class)
class GuestSearchControllerTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private FakeTokenVerifier verifier;

    private String guestToken;
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
    void searchListingsNoFilters() throws Exception {
        mockMvc.perform(get("/api/guest/listings")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].property.title").value("Beach House"));
    }

    @Test
    void searchListingsWithDateRange() throws Exception {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        mockMvc.perform(get("/api/guest/listings")
                .param("startDate", start.toString())
                .param("endDate", end.toString())
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchInvalidDateRange() throws Exception {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.minusDays(1);
        mockMvc.perform(get("/api/guest/listings")
                .param("startDate", start.toString())
                .param("endDate", end.toString())
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void searchPastStartDate() throws Exception {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        mockMvc.perform(get("/api/guest/listings")
                .param("startDate", start.toString())
                .param("endDate", end.toString())
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void searchInvalidGuestCount() throws Exception {
        mockMvc.perform(get("/api/guest/listings")
                .param("guests", "0")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk());
    }

    @Test
    void getListingDetail() throws Exception {
        mockMvc.perform(get("/api/guest/listings/" + activeProperty.getId())
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.property.title").value("Beach House"))
            .andExpect(jsonPath("$.hostDisplayName").value("Bob Host"))
            .andExpect(jsonPath("$.reviewCount").value(0));
    }

    @Test
    void getDetailInactiveListing() throws Exception {
        Instant now = Instant.now();
        Property inactive = propertyRepository.save(new Property(
                UUID.randomUUID(), hostId, ListingStatus.INACTIVE,
                "Hidden Place", "Not shown", PropertyType.APARTMENT,
                "999 Hidden St", "Singapore", "West",
                654321, 2, 1, 1, new BigDecimal("100.0000"),
                LocalTime.of(14, 0), LocalTime.of(11, 0),
                Set.of(), now));

        mockMvc.perform(get("/api/guest/listings/" + inactive.getId())
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LISTING_NOT_FOUND"));
    }

    @Test
    void getDetailNonexistentListing() throws Exception {
        mockMvc.perform(get("/api/guest/listings/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LISTING_NOT_FOUND"));
    }

    @Test
    void estimateCost() throws Exception {
        LocalDate start = LocalDate.now().plusDays(10);
        LocalDate end = start.plusDays(3);
        mockMvc.perform(get("/api/guest/listings/" + activeProperty.getId() + "/estimate")
                .param("startDate", start.toString())
                .param("endDate", end.toString())
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nights").value(3))
            .andExpect(jsonPath("$.nightlyRate").value(150.0000))
            .andExpect(jsonPath("$.totalAmount").value(450.0000));
    }

    @Test
    void estimateMissingDates() throws Exception {
        mockMvc.perform(get("/api/guest/listings/" + activeProperty.getId() + "/estimate")
                .header("Authorization", "Bearer " + guestToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/guest/listings"))
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
