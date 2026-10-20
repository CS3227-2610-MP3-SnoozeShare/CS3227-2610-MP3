package com.snoozeshare;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/** Deliberate failure to prove report artifacts upload (W-INFRA T7 step 6); reverted right after. */
class ThrowawayFailureTest {

    @Test
    void fails() {
        fail("throwaway");
    }
}
