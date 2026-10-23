package com.snoozeshare.controller.guest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.snoozeshare.PostgresIntegrationTest;
import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.LlmCall;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.LlmGateway;
import com.snoozeshare.ai.LlmResult;
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
@Import(GuestAiSearchControllerTest.TestAuthConfig.class)
class GuestAiSearchControllerTest extends PostgresIntegrationTest {

    private static final String AI_SEARCH_URL = "/api/guest/ai/search";

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

    @MockitoBean
    private LlmGateway gateway;

    private String guestToken;
    private String hostToken;
    private String agentToken;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();

        // Guest
        String guestUid = "guest-uid-" + UUID.randomUUID();
        guestToken = "guest-token-" + UUID.randomUUID();
        UUID guestId = UUID.randomUUID();
        verifier.add(guestToken, guestUid, "guest@test.com");
        User guest = userRepository.save(new User(
            guestId, guestUid, Role.GUEST, "Alice Guest",
            "guest-" + UUID.randomUUID() + "@test.com",
            AccountStatus.ACTIVE, null, now, now, null));
        walletRepository.save(new Wallet(
            UUID.randomUUID(), guest.getId(),
            new BigDecimal("1000.00"), "SGD", now));

        // Host
        String hostUid = "host-uid-" + UUID.randomUUID();
        hostToken = "host-token-" + UUID.randomUUID();
        UUID hostId = UUID.randomUUID();
        verifier.add(hostToken, hostUid, "host@test.com");
        User host = userRepository.save(new User(
            hostId, hostUid, Role.HOST, "Bob Host",
            "host-" + UUID.randomUUID() + "@test.com",
            AccountStatus.ACTIVE, "HOST123", now, now, null));
        walletRepository.save(new Wallet(
            UUID.randomUUID(), host.getId(),
            new BigDecimal("500.00"), "SGD", now));

        // Agent
        String agentUid = "agent-uid-" + UUID.randomUUID();
        agentToken = "agent-token-" + UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        verifier.add(agentToken, agentUid, "agent@test.com");
        userRepository.save(new User(
            agentId, agentUid, Role.AGENT, "Charlie Agent",
            "agent-" + UUID.randomUUID() + "@test.com",
            AccountStatus.ACTIVE, "AGENT123", now, now, null));

        // Active property
        propertyRepository.save(new Property(
            UUID.randomUUID(), hostId, ListingStatus.ACTIVE,
            "Beach House", "A lovely beach house",
            PropertyType.APARTMENT, "123 Beach Rd", "Singapore",
            "East", 123456, 4, 2, 1,
            new BigDecimal("150.0000"),
            LocalTime.of(14, 0), LocalTime.of(11, 0),
            Set.of(), now));
    }

    /** F2, N1: happy path — gateway returns ok, results contain the matching property. */
    @Test
    void aiSearchHappyPath() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            "Singapore", null, null, 2, null,
            "Looking for a place in Singapore for 2 guests.");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"2 guests in Singapore\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(true))
            .andExpect(jsonPath("$.explanation").value(filters.explanation()))
            .andExpect(jsonPath("$.filters.city").value("Singapore"))
            .andExpect(jsonPath("$.results[0].property.title").value("Beach House"))
            .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    /** E1: blank query returns 400. */
    @Test
    void blankQueryReturns400() throws Exception {
        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    /** E2: 500-char query is accepted. */
    @Test
    void maxLengthQueryAccepted() throws Exception {
        String query = "a".repeat(500);
        AiSearchFilters filters = new AiSearchFilters(
            null, null, null, null, null, "Searching for accommodation.");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"" + query + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(true));
    }

    /** E3: 501-char query returns 400. */
    @Test
    void tooLongQueryReturns400() throws Exception {
        String query = "a".repeat(501);
        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"" + query + "\"}"))
            .andExpect(status().isBadRequest());
    }

    /** E22: AI disabled returns degraded 200. */
    @Test
    void aiDisabledReturnsDegraded() throws Exception {
        mockGatewayDegraded(LlmErrorCode.DISABLED);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"something\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("DISABLED"))
            .andExpect(jsonPath("$.results").isEmpty());
    }

    /** E13: past start date from LLM returns degraded with INVALID_SEARCH_DATES. */
    @Test
    void pastStartDateReturnsDegraded() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            null, LocalDate.now().minusDays(5), LocalDate.now().plusDays(2),
            null, null, "Past date search.");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"last week\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_SEARCH_DATES"));
    }

    /** E14: reversed dates from LLM returns degraded with INVALID_SEARCH_DATES. */
    @Test
    void reversedDatesReturnsDegraded() throws Exception {
        LocalDate future = LocalDate.now().plusDays(10);
        AiSearchFilters filters = new AiSearchFilters(
            null, future.plusDays(5), future,
            null, null, "Reversed dates.");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"next week reversed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_SEARCH_DATES"));
    }

    /** E28: unauthenticated returns 401. */
    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(post(AI_SEARCH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"something\"}"))
            .andExpect(status().isUnauthorized());
    }

    /** E29: host role returns 403. */
    @Test
    void hostRoleReturns403() throws Exception {
        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + hostToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"something\"}"))
            .andExpect(status().isForbidden());
    }

    /** E30: agent role returns 403. */
    @Test
    void agentRoleReturns403() throws Exception {
        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + agentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"something\"}"))
            .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    @SuppressWarnings("unchecked")
    private void mockGatewayOk(AiSearchFilters filters) {
        when(gateway.run(any(AiFeature.class), any(LlmCall.class)))
            .thenReturn(LlmResult.ok(filters, false, false, UUID.randomUUID()));
    }

    @SuppressWarnings("unchecked")
    private void mockGatewayDegraded(LlmErrorCode code) {
        AiSearchFilters fallback = new AiSearchFilters(
            null, null, null, null, null,
            "I couldn't understand that query. Please try the manual filters.");
        when(gateway.run(any(AiFeature.class), any(LlmCall.class)))
            .thenReturn(LlmResult.degraded(fallback, code, false, false, UUID.randomUUID()));
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
