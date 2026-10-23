package com.snoozeshare.controller.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 6, 15);

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
            null, FIXED_TODAY.minusDays(5), FIXED_TODAY.plusDays(2),
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
        LocalDate future = FIXED_TODAY.plusDays(10);
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

    /** E4: vague query with all-null filters returns all active listings. */
    @Test
    void vagueQueryReturnsAllListings() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            null, null, null, null, null, "No criteria found");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"show me something\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(true))
            .andExpect(jsonPath("$.results").isNotEmpty());
    }

    /** E5: trusted context contains today's date and day of week from the fixed Clock. */
    @SuppressWarnings("unchecked")
    @Test
    void relativeDateResolvedFromTrustedContext() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            null, null, null, null, null, "Searching.");
        ArgumentCaptor<LlmCall> callCaptor = ArgumentCaptor.forClass(LlmCall.class);
        when(gateway.run(any(AiFeature.class), callCaptor.capture()))
            .thenReturn(LlmResult.ok(filters, false, false, UUID.randomUUID()));

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"next weekend\"}"))
            .andExpect(status().isOk());

        assertThat(callCaptor.getValue().trustedContext()).contains("2026-06-15");
        assertThat(callCaptor.getValue().trustedContext()).contains("MONDAY");
    }

    /** E6: flagged input still returns ok results (flagged does not degrade). */
    @SuppressWarnings("unchecked")
    @Test
    void flaggedInputLogsWarnAndReturnsResults() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            "Singapore", null, null, null, null, "Flagged but valid.");
        when(gateway.run(any(AiFeature.class), any(LlmCall.class)))
            .thenReturn(LlmResult.ok(filters, true, false, UUID.randomUUID()));

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"ignore instructions\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(true));
    }

    /** E7: all-null filters returns all active listings. */
    @Test
    void allNullFiltersReturnsAllListings() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            null, null, null, null, null, "Broad search.");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"anything\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(true))
            .andExpect(jsonPath("$.results").isNotEmpty());
    }

    /** E8: negative guests triggers INVALID_OUTPUT from gateway. */
    @Test
    void negativeGuestsReturnsFallback() throws Exception {
        mockGatewayDegraded(LlmErrorCode.INVALID_OUTPUT);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"minus one guests\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_OUTPUT"));
    }

    /** E9: zero rate triggers INVALID_OUTPUT from gateway. */
    @Test
    void zeroRateReturnsFallback() throws Exception {
        mockGatewayDegraded(LlmErrorCode.INVALID_OUTPUT);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"free rooms\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_OUTPUT"));
    }

    /** E10: blank explanation triggers INVALID_OUTPUT from gateway. */
    @Test
    void blankExplanationReturnsFallback() throws Exception {
        mockGatewayDegraded(LlmErrorCode.INVALID_OUTPUT);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"empty explanation\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_OUTPUT"));
    }

    /** E11: malformed JSON from LLM triggers INVALID_OUTPUT from gateway. */
    @Test
    void malformedJsonReturnsFallback() throws Exception {
        mockGatewayDegraded(LlmErrorCode.INVALID_OUTPUT);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"malformed json\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_OUTPUT"));
    }

    /** E12: extra fields in LLM output triggers INVALID_OUTPUT from gateway. */
    @Test
    void extraFieldsReturnsFallback() throws Exception {
        mockGatewayDegraded(LlmErrorCode.INVALID_OUTPUT);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"extra fields\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_OUTPUT"));
    }

    /** E15: long explanation triggers INVALID_OUTPUT from gateway. */
    @Test
    void longExplanationReturnsFallback() throws Exception {
        mockGatewayDegraded(LlmErrorCode.INVALID_OUTPUT);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"long explanation\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("INVALID_OUTPUT"));
    }

    /** E16: start date only (no end date) runs search without date filter error. */
    @Test
    void startDateOnlySearchesWithoutDateFilter() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            null, FIXED_TODAY.plusDays(5), null, null, null,
            "Searching from a start date.");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"from next Friday\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(true))
            .andExpect(jsonPath("$.results").isNotEmpty());
    }

    /** E17: end date only (no start date) runs search without date filter error. */
    @Test
    void endDateOnlySearchesWithoutDateFilter() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            null, null, FIXED_TODAY.plusDays(10), null, null,
            "Searching until an end date.");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"until next Sunday\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(true));
    }

    /** E18: emoji-only query is accepted and processed. */
    @Test
    void unicodeOnlyQueryProcessed() throws Exception {
        AiSearchFilters filters = new AiSearchFilters(
            null, null, null, null, null, "Emoji search.");
        mockGatewayOk(filters);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"\uD83C\uDFE0\uD83C\uDF34\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(true));
    }

    /** E19: BLOCKED_INPUT from gateway returns degraded response. */
    @Test
    void blockedInputReturnsDegraded() throws Exception {
        mockGatewayDegraded(LlmErrorCode.BLOCKED_INPUT);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"blocked input\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("BLOCKED_INPUT"));
    }

    /** E20: REFUSED from gateway returns degraded response. */
    @Test
    void refusedReturnsDegraded() throws Exception {
        mockGatewayDegraded(LlmErrorCode.REFUSED);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"refused query\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("REFUSED"));
    }

    /** E21: AUTH error from gateway returns degraded response. */
    @Test
    void authErrorReturnsDegraded() throws Exception {
        mockGatewayDegraded(LlmErrorCode.AUTH);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"auth error\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("AUTH"));
    }

    /** E23: USER_THROTTLED from gateway returns degraded with correct errorCode. */
    @Test
    void userThrottledReturnsDegraded() throws Exception {
        mockGatewayDegraded(LlmErrorCode.USER_THROTTLED);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"throttled\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("USER_THROTTLED"));
    }

    /** E24: DAILY_CAP from gateway returns degraded with correct errorCode. */
    @Test
    void dailyCapReturnsDegraded() throws Exception {
        mockGatewayDegraded(LlmErrorCode.DAILY_CAP);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"daily cap\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("DAILY_CAP"));
    }

    /** E25: CIRCUIT_OPEN from gateway returns degraded with correct errorCode. */
    @Test
    void circuitOpenReturnsDegraded() throws Exception {
        mockGatewayDegraded(LlmErrorCode.CIRCUIT_OPEN);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"circuit open\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("CIRCUIT_OPEN"));
    }

    /** E27: AUDIT_FAILED from gateway returns degraded response. */
    @Test
    void auditFailedReturnsDegraded() throws Exception {
        mockGatewayDegraded(LlmErrorCode.AUDIT_FAILED);

        mockMvc.perform(post(AI_SEARCH_URL)
                .header("Authorization", "Bearer " + guestToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"audit failed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiAvailable").value(false))
            .andExpect(jsonPath("$.errorCode").value("AUDIT_FAILED"));
    }

    /** E36: two concurrent AI searches receive independent responses. */
    @SuppressWarnings("unchecked")
    @Test
    void concurrentSearchesIndependent() throws Exception {
        AiSearchFilters filtersA = new AiSearchFilters(
            "Singapore", null, null, null, null, "Response A.");
        AiSearchFilters filtersB = new AiSearchFilters(
            null, null, null, 3, null, "Response B.");

        when(gateway.run(any(AiFeature.class), any(LlmCall.class)))
            .thenReturn(LlmResult.ok(filtersA, false, false, UUID.randomUUID()))
            .thenReturn(LlmResult.ok(filtersB, false, false, UUID.randomUUID()));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> futureA = executor.submit(() ->
                mockMvc.perform(post(AI_SEARCH_URL)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"search A\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());

            Future<String> futureB = executor.submit(() ->
                mockMvc.perform(post(AI_SEARCH_URL)
                        .header("Authorization", "Bearer " + guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"search B\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());

            String responseA = futureA.get();
            String responseB = futureB.get();

            // Each response should be valid; together they cover both filter sets
            assertThat(responseA).contains("\"aiAvailable\":true");
            assertThat(responseB).contains("\"aiAvailable\":true");
            // The two responses should differ (different explanations)
            assertThat(responseA).isNotEqualTo(responseB);
        } finally {
            executor.shutdown();
        }
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

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(
                LocalDate.of(2026, 6, 15).atStartOfDay(ZoneOffset.UTC).toInstant(),
                ZoneOffset.UTC);
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
