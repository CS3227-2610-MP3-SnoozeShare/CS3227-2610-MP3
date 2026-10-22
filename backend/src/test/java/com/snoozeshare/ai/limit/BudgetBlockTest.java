package com.snoozeshare.ai.limit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.snoozeshare.ai.limit.CircuitBreaker.Admission;

/** E19 and E47: block, probe, extension, cap at 00:00 UTC under a non-UTC clock. */
class BudgetBlockTest {

    private static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    /** A settable clock in the Singapore zone. */
    private static final class TestClock extends Clock {
        private Instant now;

        TestClock(String start) {
            now = Instant.parse(start);
        }

        void advanceMs(long ms) {
            now = now.plusMillis(ms);
        }

        @Override
        public ZoneId getZone() {
            return SINGAPORE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void blockThenProbeAfterTheWindow() {
        TestClock clock = new TestClock("2026-10-22T10:00:00Z");
        BudgetBlock block = new BudgetBlock(clock, 900_000);
        assertThat(block.tryAdmit()).isEqualTo(Admission.ALLOWED);
        block.recordBudgetExhausted();
        assertThat(block.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(899_999);
        assertThat(block.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(1);
        assertThat(block.tryAdmit()).isEqualTo(Admission.PROBE);
        assertThat(block.tryAdmit()).isEqualTo(Admission.REJECTED);
        block.recordOtherResponse(true);
        assertThat(block.tryAdmit()).isEqualTo(Admission.ALLOWED);
    }

    @Test
    void repeatedBudgetResponseExtendsTheBlock() {
        TestClock clock = new TestClock("2026-10-22T10:00:00Z");
        BudgetBlock block = new BudgetBlock(clock, 900_000);
        block.recordBudgetExhausted();
        clock.advanceMs(900_000);
        assertThat(block.tryAdmit()).isEqualTo(Admission.PROBE);
        block.recordBudgetExhausted();
        assertThat(block.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(899_999);
        assertThat(block.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(1);
        assertThat(block.tryAdmit()).isEqualTo(Admission.PROBE);
    }

    @Test
    void nonProbeResponsesDoNotLiftTheBlock() {
        TestClock clock = new TestClock("2026-10-22T10:00:00Z");
        BudgetBlock block = new BudgetBlock(clock, 900_000);
        block.recordBudgetExhausted();
        block.recordOtherResponse(false);
        assertThat(block.tryAdmit()).isEqualTo(Admission.REJECTED);
    }

    @Test
    void abandonedProbeIsGivenBack() {
        TestClock clock = new TestClock("2026-10-22T10:00:00Z");
        BudgetBlock block = new BudgetBlock(clock, 900_000);
        block.recordBudgetExhausted();
        clock.advanceMs(900_000);
        assertThat(block.tryAdmit()).isEqualTo(Admission.PROBE);
        block.abandonProbe();
        assertThat(block.tryAdmit()).isEqualTo(Admission.PROBE);
    }

    @Test
    void cappedAtTheNextMidnightUtc() {
        // 23:50 UTC with a 15 minute block would end 00:05; it ends at 00:00 UTC instead
        TestClock clock = new TestClock("2026-10-22T23:50:00Z");
        BudgetBlock block = new BudgetBlock(clock, 900_000);
        block.recordBudgetExhausted();
        clock.advanceMs(599_999);
        assertThat(block.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(1);
        assertThat(block.tryAdmit()).isEqualTo(Admission.PROBE);
    }

    @Test
    void utcBoundary() {
        // 23:59:59 UTC is 07:59:59 in Singapore; the block still ends at 00:00:00 UTC, not Singapore midnight
        TestClock clock = new TestClock("2026-10-22T23:59:59Z");
        assertThat(clock.instant().atZone(SINGAPORE).getHour()).isEqualTo(7);
        BudgetBlock block = new BudgetBlock(clock, 3_600_000);
        block.recordBudgetExhausted();
        clock.advanceMs(999);
        assertThat(block.tryAdmit()).isEqualTo(Admission.REJECTED);
        clock.advanceMs(1);
        assertThat(block.tryAdmit()).isEqualTo(Admission.PROBE);
    }
}
