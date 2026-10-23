package com.snoozeshare.service.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.domain.model.TicketAssistant.State;
import com.snoozeshare.repository.MessageRepository;
import com.snoozeshare.repository.TicketAssistantRepository;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.AgentTestUsers;
import com.snoozeshare.testsupport.DisputeScenarioFactory.Scenario;

/**
 * E35: the whole feature on the real gateway in mock mode (no credentials, no spy, no external transport): real
 * events, runner, applier, audit and API.
 */
@SpringBootTest(properties = {"snoozeshare.security.enabled=true", "snoozeshare.assistant.enabled=true",
    "snoozeshare.ai.enabled=true", "snoozeshare.ai.mock=true"})
class AssistantMockEndToEndTest extends AgentApiIntegrationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String REPLY = "unique-guest-reply-text";
    @Autowired private AssistantRunner runner;
    @Autowired private TicketAssistantRepository assistants;
    @Autowired private MessageRepository messages;

    private static String token(Role role, UUID id) {
        return "Bearer agent-api-" + role.name().toLowerCase() + "-" + id;
    }

    private void idle() {
        assertThat(runner.awaitIdle(Duration.ofSeconds(30))).isTrue();
    }

    private MvcResult guestPost(Scenario s, String json) throws Exception {
        return mockMvc.perform(post("/api/guest/messages/tickets/" + s.ticket().getId())
            .header("Authorization", token(Role.GUEST, s.guest().getId()))
            .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn();
    }

    private List<Message> thread(UUID id) {
        return messages.findThread(id, ThreadChannel.GUEST);
    }

    @Test
    void ticketToClosingWithRealPipelineAndSafeAuditE35() throws Exception {
        Scenario s = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID id = s.ticket().getId();
        idle();
        assertThat(assistants.findById(id).orElseThrow().getState()).isEqualTo(State.WAITING_GUEST);
        assertThat(thread(id)).singleElement().satisfies(m -> {
            assertThat(m.getAuthorRole()).isEqualTo(Role.SYSTEM);
            assertThat(m.getBody()).contains("On which date did the problem first happen?");
        });

        MvcResult detail = mockMvc.perform(get("/api/agent/disputes/" + id)
            .header("Authorization", token(Role.AGENT, s.agent().getId()))).andExpect(status().isOk()).andReturn();
        JsonNode assistant = MAPPER.readTree(detail.getResponse().getContentAsString()).get("assistant");
        assertThat(assistant.get("state").asText()).isEqualTo("WAITING_GUEST");
        assertThat(assistant.get("draft").asText()).contains("Mock summary.");

        for (int round = 0; round < 3; round++) {
            assertThat(guestPost(s, "{\"body\":\"" + REPLY + round + "\"}").getResponse().getStatus()).isEqualTo(200);
            idle();
        }
        var row = assistants.findById(id).orElseThrow();
        assertThat(row.getState()).isEqualTo(State.DONE);
        assertThat(row.getRounds()).isEqualTo((short) 3);
        assertThat(thread(id).stream().filter(m -> ClarifyRendering.isClosing(m.getBody()))).hasSize(1);

        assertThat(jdbc.queryForObject("select count(*) from audit_log where action_type = 'AI_CALL' "
            + "and ticket_id = ?", Integer.class, id)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from audit_log where action_type = 'ASSISTANT_MESSAGE' "
            + "and ticket_id = ?", Integer.class, id)).isEqualTo(4);
        List<String> rows = jdbc.queryForList("select a::text from audit_log a where ticket_id = ?", String.class, id);
        assertThat(rows).noneMatch(r -> r.contains("Mock summary") || r.contains(REPLY)
            || r.contains("On which date"));
    }

    @Test
    void spoofedRoleAndForeignTicketAreRejectedWithoutAJobE44E45() throws Exception {
        Scenario s = scenarios.openDispute(RemedyType.FULL_REFUND);
        UUID id = s.ticket().getId();
        idle();
        int before = thread(id).size();
        MvcResult spoof = guestPost(s, "{\"body\":\"x\",\"authorRole\":\"SYSTEM\"}");
        idle();
        List<Message> after = thread(id);
        assertThat(after.stream().filter(m -> "x".equals(m.getBody())))
            .allMatch(m -> m.getAuthorRole() == Role.GUEST);
        assertThat(after.stream().noneMatch(m -> m.getAuthorRole() == Role.SYSTEM && "x".equals(m.getBody())))
            .isTrue();
        assertThat(after.size()).isGreaterThanOrEqualTo(before);

        AgentTestUsers.TestUser other = testUsers.create(Role.GUEST);
        var state = assistants.findById(id).orElseThrow();
        int calls = jdbc.queryForObject("select count(*) from audit_log where action_type = 'AI_CALL' "
            + "and ticket_id = ?",
            Integer.class, id);
        MvcResult foreign = mockMvc.perform(post("/api/guest/messages/tickets/" + id)
            .header("Authorization", "Bearer " + other.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"intruder\"}")).andReturn();
        idle();
        assertThat(foreign.getResponse().getStatus()).isGreaterThanOrEqualTo(400);
        assertThat(thread(id).stream().noneMatch(m -> "intruder".equals(m.getBody()))).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from audit_log where action_type = 'AI_CALL' "
            + "and ticket_id = ?", Integer.class, id)).isEqualTo(calls);
        assertThat(assistants.findById(id).orElseThrow().getRounds()).isEqualTo(state.getRounds());
    }
}
