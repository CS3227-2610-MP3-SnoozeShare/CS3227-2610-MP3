package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.TicketAssistant;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.LedgerRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.TicketAssistantRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.impl.DisputeQueryServiceImpl;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** Agent-only projection, independent of the assistant runtime. */
@SpringBootTest(properties = {"snoozeshare.security.enabled=true", "snoozeshare.assistant.enabled=false"})
class AssistantReadApiTest extends AgentApiIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final String DRAFT = "Private assistant draft";
    @Autowired private ObjectMapper mapper;
    @Autowired private TicketAssistantRepository assistants;
    @Autowired private TicketRepository tickets;
    @Autowired private BookingRepository bookings;
    @Autowired private PropertyRepository properties;
    @Autowired private UserRepository users;
    @Autowired private LedgerRepository ledger;
    @Autowired private Environment environment;

    @Test
    void absentRowIsAnExplicitNull() throws Exception { // E23
        var scenario = scenarios.openDispute(RemedyType.FULL_REFUND);
        JsonNode response = read(path(scenario.ticket().getId()), agent.token());
        assertThat(response.has("assistant")).isTrue();
        assertThat(response.get("assistant").isNull()).isTrue();
    }

    @Test
    void allStatesExposeExactlySevenFieldsWithRuntimeDisabled() throws Exception { // E43
        assertThat(environment.getProperty("snoozeshare.assistant.enabled", Boolean.class, true)).isFalse();
        for (TicketAssistant.State state : TicketAssistant.State.values()) {
            var scenario = scenarios.openDispute(RemedyType.FULL_REFUND);
            TicketAssistant row = row(scenario.ticket().getId(), state, NOW);
            row.setRounds((short) 2);
            row.setFlagged(true);
            row.setDraft(DRAFT);
            row.setDraftUpdatedAt(NOW);
            assistants.saveAndFlush(row);
            JsonNode assistant = read(path(row.getTicketId()), agent.token()).get("assistant");
            List<String> keys = new ArrayList<>();
            assistant.fieldNames().forEachRemaining(keys::add);
            assertThat(keys).containsExactlyInAnyOrder("state", "rounds", "maxRounds", "flagged", "stale",
                    "draft", "draftUpdatedAt");
            assertThat(assistant.get("state").asText()).isEqualTo(state.name());
            assertThat(assistant.get("rounds").asInt()).isEqualTo(2);
            assertThat(assistant.get("maxRounds").asInt()).isEqualTo(3);
            assertThat(assistant.get("flagged").asBoolean()).isTrue();
            assertThat(assistant.get("draft").asText()).isEqualTo(DRAFT);
            assertThat(assistant.get("draftUpdatedAt").asText()).isEqualTo(NOW.toString());
        }
    }

    @Test
    void staleUsesStrictBoundaryInjectedClockAndConfiguredThreshold() { // E24, E59
        var scenario = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID id = scenario.ticket().getId();
        var defaults = new DisputeQueryServiceImpl(tickets, bookings, properties, users, ledger,
                Clock.fixed(NOW, ZoneOffset.UTC), assistants, new MockEnvironment());
        for (long seconds : new long[] {601, 600, -60}) {
            assistants.saveAndFlush(row(id, TicketAssistant.State.PENDING, NOW.minusSeconds(seconds)));
            assertThat(defaults.detail(id).assistant().stale()).isEqualTo(seconds > 600);
        }
        var query = new DisputeQueryServiceImpl(tickets, bookings, properties, users, ledger,
                Clock.fixed(NOW, ZoneOffset.UTC), assistants, new MockEnvironment()
                        .withProperty("snoozeshare.assistant.max-rounds", "5")
                        .withProperty("snoozeshare.assistant.stale-after-minutes", "7"));
        for (long seconds : new long[] {421, 420, 419, -60}) {
            assistants.saveAndFlush(row(id, TicketAssistant.State.PENDING, NOW.minusSeconds(seconds)));
            assertThat(query.detail(id).assistant().stale()).isEqualTo(seconds > 420);
            assertThat(query.detail(id).assistant().maxRounds()).isEqualTo(5);
        }
        for (TicketAssistant.State state : TicketAssistant.State.values()) {
            assistants.saveAndFlush(row(id, state, NOW.minusSeconds(1000)));
            assertThat(query.detail(id).assistant().stale()).isEqualTo(state == TicketAssistant.State.PENDING);
        }
        var resolved = scenarios.resolvedDispute(agent.id());
        assistants.saveAndFlush(row(resolved.ticket().getId(), TicketAssistant.State.PENDING, NOW.minusSeconds(1000)));
        assertThat(query.detail(resolved.ticket().getId()).assistant().stale()).isFalse();
    }

    @Test
    void authenticationAndUnknownTicketErrorsStayUnchanged() throws Exception { // E40, E41, E42
        var scenario = scenarios.openDispute(RemedyType.FULL_REFUND);
        String path = path(scenario.ticket().getId());
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        for (String token : List.of(guest.token(), host.token())) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(path(UUID.randomUUID())).header("Authorization", "Bearer " + agent.token()))
                .andExpect(status().isNotFound());
        var otherAgent = testUsers.create(Role.AGENT);
        read(path, otherAgent.token());
    }

    @Test
    void actionResponsesKeepProjectionAndPublicRoleResponsesCannotLeakIt() throws Exception { // E43
        var scenario = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID id = scenario.ticket().getId();
        TicketAssistant row = row(id, TicketAssistant.State.DONE, NOW);
        row.setDraft(DRAFT);
        assistants.saveAndFlush(row);
        for (String action : List.of("assign", "notes", "unassign", "assign", "resolve")) {
            String body = switch (action) {
                case "notes" -> "{\"notes\":\"Agent note\"}";
                case "resolve" -> "{\"mode\":\"REJECT\",\"reason\":\"No evidence\"}";
                default -> "";
            };
            var result = mockMvc.perform(put(path(id) + "/" + action)
                    .header("Authorization", "Bearer " + agent.token())
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
            assertThat(mapper.readTree(result.getResponse().getContentAsString()).path("assistant").path("draft")
                    .asText()).isEqualTo(DRAFT);
        }
        String guestToken = token(scenario.guest().getId(), "guest");
        String hostToken = token(scenario.host().getId(), "host");
        for (String endpoint : List.of("/api/guest/tickets", "/api/guest/messages/conversations",
                "/api/guest/messages/tickets/" + id, "/api/guest/tickets/categories")) {
            assertPrivateFieldsAbsent(read(endpoint, guestToken));
        }
        for (String endpoint : List.of("/api/host/messages/tickets",
                "/api/host/messages/tickets/" + id + "?channel=HOST")) {
            assertPrivateFieldsAbsent(read(endpoint, hostToken));
        }
        assertPrivateFieldsAbsent(read("/api/agent/disputes", agent.token()));
    }

    private void assertPrivateFieldsAbsent(JsonNode node) {
        if (node.isObject()) {
            node.fields().forEachRemaining(field -> {
                assertThat(field.getKey()).isNotIn("assistant", "draft", "lastMessageId", "created_at",
                        "last_message_id", "prompt", "gateway", "draftUpdatedAt");
                assertPrivateFieldsAbsent(field.getValue());
            });
        } else if (node.isArray()) {
            node.forEach(this::assertPrivateFieldsAbsent);
        } else if (node.isTextual()) {
            assertThat(node.asText()).doesNotContain(DRAFT);
        }
    }

    private JsonNode read(String endpoint, String token) throws Exception {
        var result = mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private static TicketAssistant row(UUID id, TicketAssistant.State state, Instant updated) {
        TicketAssistant row = new TicketAssistant(id, updated);
        row.setState(state);
        return row;
    }

    private static String path(UUID id) {
        return "/api/agent/disputes/" + id;
    }

    private static String token(UUID id, String role) {
        return "agent-api-" + role + "-" + id;
    }
}
