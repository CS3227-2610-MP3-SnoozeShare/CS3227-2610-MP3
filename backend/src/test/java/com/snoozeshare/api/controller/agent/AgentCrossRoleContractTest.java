package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;
import com.snoozeshare.testsupport.DisputeScenarioFactory.Scenario;

/** What the agent API does must be visible, with the same shapes, through the Guest and Host APIs. */
class AgentCrossRoleContractTest extends AgentApiIntegrationTest {

    private static String bearer(User user) {
        return "Bearer agent-api-" + user.getRole().name().toLowerCase() + "-" + user.getId();
    }

    private ResultActions asAgent(MockHttpServletRequestBuilder builder,
                                  String json) throws Exception {
        builder.header("Authorization", "Bearer " + agent.token());
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder);
    }

    private ResultActions as(User user, MockHttpServletRequestBuilder builder, String json) throws Exception {
        builder.header("Authorization", bearer(user));
        if (json != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(builder);
    }

    @Test
    void agentRepliesReachGuestAndHostThreads() throws Exception {
        Scenario dispute = scenarios.inReviewDispute(agent.id());
        UUID id = dispute.ticket().getId();
        asAgent(post("/api/agent/disputes/" + id + "/messages/GUEST"), "{\"body\":\"Hello guest\"}")
                .andExpect(status().is2xxSuccessful());
        asAgent(post("/api/agent/disputes/" + id + "/messages/HOST"), "{\"body\":\"Hello host\"}")
                .andExpect(status().is2xxSuccessful());

        as(dispute.guest(), get("/api/guest/messages/tickets/" + id), null).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].body").value("Hello guest"))
                .andExpect(jsonPath("$[0].authorRole").value("AGENT"))
                .andExpect(jsonPath("$[0].sentAt").exists());
        as(dispute.host(), get("/api/host/messages/tickets/" + id + "?channel=HOST"), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].body").value("Hello host"))
                .andExpect(jsonPath("$[0].authorRole").value("AGENT"));
    }

    @Test
    void guestAndHostRepliesReachTheAgentThreads() throws Exception {
        Scenario dispute = scenarios.inReviewDispute(agent.id());
        UUID id = dispute.ticket().getId();
        as(dispute.guest(), post("/api/guest/messages/tickets/" + id), "{\"body\":\"From the guest\"}")
                .andExpect(status().is2xxSuccessful());
        as(dispute.host(), post("/api/host/messages/tickets/" + id + "?channel=HOST"), "{\"body\":\"From the host\"}")
                .andExpect(status().is2xxSuccessful());

        asAgent(get("/api/agent/disputes/" + id + "/messages/GUEST"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].body", hasItem("From the guest")))
                .andExpect(jsonPath("$[0].authorRole").value("GUEST"));
        asAgent(get("/api/agent/disputes/" + id + "/messages/HOST"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].body", hasItem("From the host")))
                .andExpect(jsonPath("$[0].authorRole").value("HOST"));
    }

    @Test
    void internalNotesNeverReachTheGuestApi() throws Exception {
        Scenario dispute = scenarios.inReviewDispute(agent.id());
        UUID id = dispute.ticket().getId();
        asAgent(put("/api/agent/disputes/" + id + "/notes"), "{\"notes\":\"secret agent note\"}")
                .andExpect(status().isOk());

        MvcResult mine = as(dispute.guest(), get("/api/guest/tickets"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IN_REVIEW")).andReturn();
        assertThat(mine.getResponse().getContentAsString()).doesNotContain("secret agent note", "agentNotes");
    }

    @Test
    void resolvingClosesTheThreadForGuestAndHost() throws Exception {
        Scenario dispute = scenarios.inReviewDispute(agent.id());
        UUID id = dispute.ticket().getId();
        asAgent(put("/api/agent/disputes/" + id + "/resolve"), "{\"mode\":\"REJECT\",\"reason\":\"No evidence\"}")
                .andExpect(status().isOk());

        as(dispute.guest(), post("/api/guest/messages/tickets/" + id), "{\"body\":\"too late\"}")
                .andExpect(status().is4xxClientError());
        as(dispute.host(), post("/api/host/messages/tickets/" + id + "?channel=HOST"), "{\"body\":\"too late\"}")
                .andExpect(status().is4xxClientError());
        as(dispute.guest(), get("/api/guest/tickets"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RESOLVED_REJECTED"))
                .andExpect(jsonPath("$[0].resolutionReason").value("No evidence"));
    }

    @Test
    void deactivatedCategoriesLeaveTheGuestFilingForm() throws Exception {
        Scenario dispute = scenarios.openDispute(RemedyType.FULL_REFUND);
        String category = dispute.category();
        UUID categoryId = jdbc.queryForObject("select id from ticket_categories where label = ?", UUID.class, category);

        as(dispute.guest(), get("/api/guest/tickets/categories"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem(category)));
        asAgent(put("/api/agent/categories/" + categoryId + "/active"), "{\"active\":false}")
                .andExpect(status().isOk());

        as(dispute.guest(), get("/api/guest/tickets/categories"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$", not(hasItem(category))));
    }

    @Test
    void agentQueueShowsOpenTicketsWithTheGuestFields() throws Exception {
        Scenario dispute = scenarios.openDispute(RemedyType.PARTIAL_REFUND);

        asAgent(get("/api/agent/disputes"), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ticketId").value(dispute.ticket().getId().toString()))
                .andExpect(jsonPath("$.items[0].status").value("OPEN"))
                .andExpect(jsonPath("$.unassignedCount").value(1));
        asAgent(get("/api/agent/disputes/" + dispute.ticket().getId()), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.requestedRemedy").value("PARTIAL_REFUND"))
                .andExpect(jsonPath("$.ticket.raisedByRole").value("GUEST"));
    }
}
