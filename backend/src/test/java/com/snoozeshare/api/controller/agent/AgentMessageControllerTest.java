package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.ErrorAssertions;

/** A6 and A7: the guest and host threads of a dispute. */
class AgentMessageControllerTest extends AgentApiIntegrationTest {

    private String thread(UUID ticketId, String channel) {
        return "/api/agent/disputes/" + ticketId + "/messages/" + channel;
    }

    private ResultActions read(String path) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + agent.token()));
    }

    private ResultActions send(String path, String body) throws Exception {
        return mockMvc.perform(post(path).header("Authorization", "Bearer " + agent.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + body + "\"}"));
    }

    @Test
    void postAndReadBothChannels() throws Exception { // F8
        var dispute = scenarios.inReviewDispute(agent.id());
        UUID id = dispute.ticket().getId();

        send(thread(id, "HOST"), "Please confirm the noise level")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorRole").value("AGENT"))
                .andExpect(jsonPath("$.authorId").value(agent.id().toString()));
        read(thread(id, "HOST")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Please confirm the noise level"));
        read(thread(id, "GUEST")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void threadsHoldNoNotes() throws Exception { // E17
        var dispute = scenarios.inReviewDispute(agent.id());
        UUID id = dispute.ticket().getId();
        mockMvc.perform(put("/api/agent/disputes/" + id + "/notes").header("Authorization", "Bearer " + agent.token())
                .contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"secret investigation note\"}"))
                .andExpect(status().isOk());

        for (String channel : new String[] {"GUEST", "HOST"}) {
            String body = read(thread(id, channel)).andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("secret investigation note");
        }
    }

    @Test
    void resolvedTicketIs409() throws Exception { // E39
        var dispute = scenarios.resolvedDispute(agent.id());

        var result = send(thread(dispute.ticket().getId(), "GUEST"), "too late")
                .andExpect(status().isConflict()).andReturn();

        ErrorAssertions.assertCleanMessage(result, "Ticket is resolved");
        read(thread(dispute.ticket().getId(), "GUEST")).andExpect(status().isOk());
    }

    @Test
    void blankAndLongBodies() throws Exception { // E51, E65
        var dispute = scenarios.openDispute(RemedyType.FULL_REFUND);
        String path = thread(dispute.ticket().getId(), "GUEST");

        send(path, "").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        send(path, "x".repeat(2001)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        send(path, "x".repeat(2000)).andExpect(status().isCreated());
    }

    @Test
    void badChannel() throws Exception { // E9
        var dispute = scenarios.openDispute(RemedyType.FULL_REFUND);

        read(thread(dispute.ticket().getId(), "BOGUS")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownTicketIs404() throws Exception { // E66
        UUID unknown = UUID.randomUUID();

        var read = read(thread(unknown, "GUEST")).andExpect(status().isNotFound()).andReturn();
        var sent = send(thread(unknown, "HOST"), "hello").andExpect(status().isNotFound()).andReturn();

        ErrorAssertions.assertCleanMessage(read, "Ticket does not exist");
        ErrorAssertions.assertCleanMessage(sent, "Ticket does not exist");
    }
}
