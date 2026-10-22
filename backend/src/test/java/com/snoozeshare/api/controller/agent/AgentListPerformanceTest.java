package com.snoozeshare.api.controller.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.snoozeshare.testsupport.AgentApiIntegrationTest;

/** E57: the three list endpoints stay usable with a few hundred tickets and accounts and thousands of audit rows. */
class AgentListPerformanceTest extends AgentApiIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(AgentListPerformanceTest.class);
    private static final int CALLS = 20;
    private static final long LIMIT_MILLIS = 5000;

    private long p95Millis(String path) throws Exception {
        List<Long> samples = new ArrayList<>();
        for (int call = 0; call < CALLS; call++) {
            long start = System.nanoTime();
            mockMvc.perform(get(path).header("Authorization", "Bearer " + agent.token()))
                    .andExpect(status().isOk());
            samples.add((System.nanoTime() - start) / 1_000_000);
        }
        Collections.sort(samples);
        return samples.get((int) Math.ceil(CALLS * 0.95) - 1);
    }

    @Test
    void listsStayUnderFiveSeconds() throws Exception { // E57
        scenarios.bulk(300, 300, 2000);

        long queue = p95Millis("/api/agent/disputes");
        long accounts = p95Millis("/api/agent/accounts");
        long audit = p95Millis("/api/agent/audit?limit=200");

        LOG.info("E57 p95 ms: queue={} accounts={} audit={}", queue, accounts, audit);
        assertThat(queue).isLessThan(LIMIT_MILLIS);
        assertThat(accounts).isLessThan(LIMIT_MILLIS);
        assertThat(audit).isLessThan(LIMIT_MILLIS);
    }
}
