package com.snoozeshare.ai.limit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.snoozeshare.ai.limit.SchedulerCore.Job;
import com.snoozeshare.ai.limit.SchedulerCore.Offered;
import com.snoozeshare.ai.limit.SchedulerCore.Status;

/** E14, E15, E16, E17 and E55 on the pure state machine, driven by explicit instants. */
class SchedulerCoreTest {

    private static final Instant T0 = Instant.parse("2026-10-22T10:00:00Z");

    @Test
    void ceilingsAndPriority() {
        SchedulerCore core = new SchedulerCore(55, 4, 200);
        List<Job> held = new ArrayList<>();
        List<Instant> startTimes = new ArrayList<>();
        for (int i = 0; i < 199; i++) {
            Job job = core.offer(2, T0, T0).job();
            if (job.status() == Status.RUNNING) {
                held.add(job);
                startTimes.add(T0);
            }
        }
        assertThat(held).hasSize(3);
        Job urgent = core.offer(0, T0.plusMillis(1), T0).job();
        assertThat(urgent.status()).isEqualTo(Status.RUNNING);
        held.add(urgent);
        startTimes.add(T0);

        int maxRunning = core.running();
        Instant now = T0;
        for (int tick = 0; tick < 600 && startTimes.size() < 200; tick++) {
            now = now.plusSeconds(1);
            held.forEach(core::release);
            held.clear();
            for (Job j : core.promote(now)) {
                held.add(j);
                startTimes.add(now);
            }
            maxRunning = Math.max(maxRunning, core.running());
        }
        assertThat(startTimes).hasSize(200);
        assertThat(maxRunning).isLessThanOrEqualTo(4);
        for (Instant t : startTimes) {
            long inWindow = startTimes.stream()
                .filter(s -> s.isAfter(t.minusSeconds(60)) && !s.isAfter(t)).count();
            assertThat(inWindow).isLessThanOrEqualTo(55);
        }
    }

    @Test
    void ceilings() {
        SchedulerCore core = new SchedulerCore(2, 8, 10);
        core.offer(0, T0, T0);
        core.offer(0, T0, T0);
        Job third = core.offer(0, T0, T0).job();
        assertThat(third.status()).isEqualTo(Status.QUEUED);
        assertThat(core.nextWindowOpening(T0)).isEqualTo(T0.plusSeconds(60));
        assertThat(core.promote(T0.plusSeconds(59))).isEmpty();
        assertThat(core.promote(T0.plusSeconds(60))).containsExactly(third);
    }

    @Test
    void ceilingsAndPriorityOrder() {
        SchedulerCore core = new SchedulerCore(55, 4, 20);
        List<Job> priorityTwo = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            priorityTwo.add(core.offer(2, T0, T0).job());
        }
        assertThat(priorityTwo.stream().filter(j -> j.status() == Status.RUNNING)).hasSize(3);
        Job waiting = priorityTwo.get(3);
        assertThat(waiting.status()).isEqualTo(Status.QUEUED);
        Job urgent = core.offer(0, T0.plusSeconds(1), T0).job();
        assertThat(urgent.status()).isEqualTo(Status.RUNNING);
        assertThat(core.running()).isEqualTo(4);
    }

    @Test
    void priorityZeroStartsBeforeAnEarlierPriorityTwoJob() {
        SchedulerCore core = new SchedulerCore(55, 2, 20);
        Job first = core.offer(0, T0, T0).job();
        Job two = core.offer(2, T0, T0).job();
        assertThat(two.status()).isEqualTo(Status.QUEUED);
        Job zero = core.offer(0, T0.plusSeconds(5), T0).job();
        assertThat(zero.status()).isEqualTo(Status.RUNNING);
        core.release(first);
        core.release(zero);
        assertThat(core.promote(T0)).containsExactly(two);
    }

    @Test
    void queueFull() {
        SchedulerCore core = new SchedulerCore(55, 2, 20);
        core.offer(0, T0, T0);
        core.offer(0, T0, T0);
        for (int i = 0; i < 20; i++) {
            assertThat(core.offer(2, T0, T0).job()).isNotNull();
        }
        assertThat(core.queued()).isEqualTo(20);
        assertThat(core.offer(2, T0, T0).job()).isNull();
        assertThat(core.offer(1, T0, T0).job()).isNull();
        assertThat(core.queued()).isEqualTo(20);
    }

    @Test
    void queueTimeout() {
        SchedulerCore core = new SchedulerCore(55, 2, 1);
        core.offer(0, T0, T0);
        core.offer(0, T0, T0);
        Job waiting = core.offer(2, T0, T0).job();
        assertThat(core.offer(2, T0, T0).job()).isNull();
        assertThat(core.cancel(waiting)).isTrue();
        assertThat(waiting.status()).isEqualTo(Status.CANCELLED);
        assertThat(core.cancel(waiting)).isFalse();
        assertThat(core.offer(2, T0, T0).job()).isNotNull();
    }

    @Test
    void retriesKeepArrival() {
        SchedulerCore core = new SchedulerCore(3, 2, 20);
        Job first = core.offer(0, T0, T0).job();
        Job second = core.offer(0, T0.plusSeconds(1), T0.plusSeconds(1)).job();
        core.release(first);
        core.release(second);
        Instant now = T0.plusSeconds(2);
        Job third = core.offer(0, now, now).job();
        assertThat(third.status()).isEqualTo(Status.RUNNING);
        core.release(third);
        // the rate ceiling (3 starts) is used up: a newer call and then a retry of the oldest call both wait
        Job newer = core.offer(0, now.plusSeconds(1), now.plusSeconds(1)).job();
        Job retry = core.offer(0, T0, now.plusSeconds(2)).job();
        assertThat(retry.arrival()).isEqualTo(T0);
        assertThat(retry.status()).isEqualTo(Status.QUEUED);
        assertThat(core.promote(T0.plusSeconds(60))).containsExactly(retry);
        assertThat(newer.status()).isEqualTo(Status.QUEUED);
        // the retry took a start of its own, counting toward the ceiling
        assertThat(core.nextWindowOpening(T0.plusSeconds(60))).isEqualTo(T0.plusSeconds(61));
    }

    @Test
    void priorityZeroReserve() {
        SchedulerCore core = new SchedulerCore(55, 2, 3);
        Job running = core.offer(2, T0, T0).job();
        assertThat(running.status()).isEqualTo(Status.RUNNING);
        Job a = core.offer(2, T0.plusSeconds(1), T0).job();
        Job b = core.offer(2, T0.plusSeconds(2), T0).job();
        Job youngest = core.offer(1, T0.plusSeconds(3), T0).job();
        assertThat(core.running()).isEqualTo(1);
        assertThat(a.status()).isEqualTo(Status.QUEUED);

        Offered urgent = core.offer(0, T0.plusSeconds(4), T0);
        assertThat(urgent.displaced()).isSameAs(youngest);
        assertThat(youngest.status()).isEqualTo(Status.DISPLACED);
        assertThat(urgent.job().status()).isEqualTo(Status.RUNNING);
        assertThat(b.status()).isEqualTo(Status.QUEUED);
    }

    @Test
    void priorityZeroCannotDisplaceWhenOnlyUrgentJobsWait() {
        SchedulerCore core = new SchedulerCore(55, 2, 1);
        core.offer(0, T0, T0);
        core.offer(0, T0, T0);
        assertThat(core.offer(0, T0, T0).job()).isNotNull();
        Offered refused = core.offer(0, T0, T0);
        assertThat(refused.job()).isNull();
        assertThat(refused.displaced()).isNull();
    }

    @Test
    void windowEdgeIsExclusive() {
        SchedulerCore core = new SchedulerCore(1, 4, 5);
        Job first = core.offer(0, T0, T0).job();
        core.release(first);
        Job second = core.offer(0, T0, T0.plusMillis(59_999)).job();
        assertThat(second.status()).isEqualTo(Status.QUEUED);
        assertThat(core.promote(T0.plusSeconds(60))).containsExactly(second);
    }
}
