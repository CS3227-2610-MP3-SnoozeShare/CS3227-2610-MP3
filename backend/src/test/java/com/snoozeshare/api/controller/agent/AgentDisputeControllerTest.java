package com.snoozeshare.api.controller.agent;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** A1 to A5 of the agent dispute API. */
class AgentDisputeControllerTest extends AgentApiIntegrationTest {

    private ResultActions getAs(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    @Test
    void queueFiltersAndCounts() throws Exception { // F3
        var first = scenarios.openDispute(RemedyType.FULL_REFUND);
        scenarios.openDispute(RemedyType.FULL_REFUND);
        scenarios.openDispute(RemedyType.FULL_REFUND);
        scenarios.inReviewDispute(agent.id());

        getAs("/api/agent/disputes", agent.token())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].ticketId").value(first.ticket().getId().toString()))
                .andExpect(jsonPath("$.unassignedCount").value(3));
        getAs("/api/agent/disputes?assignee=MINE", agent.token())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.unassignedCount").value(3));
        getAs("/api/agent/disputes?assignee=UNASSIGNED", agent.token())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    void mineIsEmptyCountStaysGlobal() throws Exception { // E52
        scenarios.openDispute(RemedyType.FULL_REFUND);
        var other = testUsers.create(com.snoozeshare.domain.enums.Role.AGENT);

        getAs("/api/agent/disputes?assignee=MINE", other.token())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.unassignedCount").value(1));
    }

    @Test
    void badAssignee() throws Exception { // E9
        getAs("/api/agent/disputes?assignee=BOGUS", agent.token())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownQueryParametersAreIgnored() throws Exception { // E80
        getAs("/api/agent/disputes?foo=1", agent.token()).andExpect(status().isOk());
    }
}
