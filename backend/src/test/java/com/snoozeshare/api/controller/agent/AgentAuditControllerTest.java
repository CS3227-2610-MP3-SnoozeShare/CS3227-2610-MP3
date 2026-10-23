package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.ErrorAssertions;

/** A11 and A17: the audit log search and its action list. */
class AgentAuditControllerTest extends AgentApiIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ResultActions search(String query) throws Exception {
        return mockMvc.perform(get("/api/agent/audit" + query)
                .header("Authorization", "Bearer " + agent.token()));
    }

    private void insert(UUID id, UUID actorId, String actorName, String action, Instant at) {
        jdbc.update("insert into audit_log (id, actor_user_id, actor_name, action_type, entity_type, entity_id, "
                + "timestamp) values (?, ?, ?, ?, 'User', ?, ?)", id, actorId, actorName, action, UUID.randomUUID(),
                java.sql.Timestamp.from(at));
    }

    @Test
    void pagesAreStableAndNewestFirst() throws Exception { // E34
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("insert into audit_log (id, actor_user_id, actor_name, action_type, entity_type, entity_id, "
                + "timestamp) select gen_random_uuid(), ?, 'Bulk', 'WITHDRAWAL', 'User', gen_random_uuid(), "
                + "?::timestamptz + (n % 5) * interval '1 minute' from generate_series(1, 450) n",
                agent.id(), base.toString());

        Set<String> seen = new HashSet<>();
        List<String> order = new ArrayList<>();
        for (int offset = 0; offset < 450; offset += 200) {
            MvcResult page = search("?actions=WITHDRAWAL&limit=200&offset=" + offset)
                    .andExpect(status().isOk()).andReturn();
            for (JsonNode row : MAPPER.readTree(page.getResponse().getContentAsString())) {
                seen.add(row.get("id").asText());
                order.add(row.get("timestamp").asText() + "|" + row.get("id").asText());
            }
        }

        assertThat(seen).hasSize(450);
        assertThat(order).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void pageBounds() throws Exception { // E30
        for (String bad : new String[] {"?limit=0", "?limit=201", "?offset=-1", "?limit=abc"}) {
            search(bad).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        search("?limit=200&offset=0").andExpect(status().isOk());
    }

    @Test
    void dayBoundaryUsesSingaporeTime() throws Exception { // E31
        insert(UUID.randomUUID(), agent.id(), "Agent", "TOP_UP", Instant.parse("2026-10-21T23:30:00Z"));

        search("?actions=TOP_UP&from=2026-10-22&to=2026-10-22").andExpect(jsonPath("$", hasSize(1)));
        search("?actions=TOP_UP&from=2026-10-21&to=2026-10-21").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void ranges() throws Exception { // E32
        insert(UUID.randomUUID(), agent.id(), "Agent", "TOP_UP", Instant.parse("2026-03-10T04:00:00Z"));

        MvcResult bad = search("?from=2026-03-11&to=2026-03-10").andExpect(status().isBadRequest()).andReturn();
        ErrorAssertions.assertCleanMessage(bad, "The From date must not be after the To date");
        search("?actions=TOP_UP&from=2026-03-10").andExpect(jsonPath("$", hasSize(1)));
        search("?actions=TOP_UP&from=2026-03-11").andExpect(jsonPath("$", hasSize(0)));
        search("?actions=TOP_UP&to=2026-03-10").andExpect(jsonPath("$", hasSize(1)));
        search("?actions=TOP_UP&to=2026-03-09").andExpect(jsonPath("$", hasSize(0)));
        search("?from=notadate").andExpect(status().isBadRequest());
    }

    @Test
    void inputRules() throws Exception { // E33
        insert(UUID.randomUUID(), agent.id(), "Agent", "TOP_UP", Instant.now());
        insert(UUID.randomUUID(), agent.id(), "Agent", "WITHDRAWAL", Instant.now());
        insert(UUID.randomUUID(), agent.id(), "Agent", "TICKET_OPENED", Instant.now());

        search("?actions=NOT_AN_ACTION").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        search("?actions=").andExpect(jsonPath("$", hasSize(3)));
        search("?actions=TOP_UP,TOP_UP, WITHDRAWAL").andExpect(jsonPath("$", hasSize(2)));
        search("?actions=" + String.join(",", java.util.Collections.nCopies(41, "TOP_UP")))
                .andExpect(status().isBadRequest());
        search("?text=" + "x".repeat(101)).andExpect(status().isBadRequest());
        search("?text=" + "x".repeat(100)).andExpect(status().isOk());
    }

    @Test
    void actionListHoldsEveryAction() throws Exception { // E71
        mockMvcActions().andExpect(status().isOk()).andExpect(jsonPath("$", hasItem("BOOKING_FORCE_CANCELLED")))
                .andExpect(jsonPath("$", hasItem("TOP_UP")));
    }

    private ResultActions mockMvcActions() throws Exception {
        return mockMvc.perform(get("/api/agent/audit/actions").header("Authorization", "Bearer " + agent.token()));
    }

    @Test
    void percentAndUnderscoreAreLiteral() throws Exception { // E82
        insert(UUID.randomUUID(), guest.id(), "50%_off", "TOP_UP", Instant.now());
        insert(UUID.randomUUID(), host.id(), "axb", "TOP_UP", Instant.now());

        search("?text=%").andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].actorName").value("50%_off"));
        search("?text=0%_o").andExpect(jsonPath("$", hasSize(1)));
        search("?text=a_b").andExpect(jsonPath("$", hasSize(0)));
        search("?text=!").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownParameterIsIgnoredAndIdsAreHidden() throws Exception { // E80
        insert(UUID.randomUUID(), agent.id(), "Agent", "TOP_UP", Instant.now());

        MvcResult result = search("?bar=2&actions=TOP_UP").andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("actorUserId", "subjectUserId");
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
    }
}
