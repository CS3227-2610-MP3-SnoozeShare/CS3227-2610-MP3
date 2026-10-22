package com.snoozeshare.ai;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.snoozeshare.ai.audit.AiAuditWriter;
import com.snoozeshare.ai.client.AttemptResult;
import com.snoozeshare.ai.client.SoclaasClient;
import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.ai.guard.OutputValidator;
import com.snoozeshare.ai.guard.PromptBuilder;
import com.snoozeshare.ai.limit.Backoff;
import com.snoozeshare.ai.limit.BudgetBlock;
import com.snoozeshare.ai.limit.CircuitBreaker;
import com.snoozeshare.ai.limit.CircuitBreaker.Admission;
import com.snoozeshare.ai.limit.LlmScheduler;
import com.snoozeshare.ai.limit.Sleeper;
import com.snoozeshare.ai.limit.UserThrottle;
import com.snoozeshare.config.ConditionalOnDatabase;

/**
 * The single entry point for every LLM call in the backend. It runs the fixed pipeline of the spec's api.md and
 * returns the feature's fallback with a code instead of throwing for any expected failure.
 *
 * <p>Callers invoke {@link #run} outside any transaction. The call blocks the calling thread for at most
 * {@code deadline-ms} plus the time of its own database calls.
 */
@Component
@ConditionalOnDatabase
public class LlmGateway {

    private static final Logger LOG = LoggerFactory.getLogger(LlmGateway.class);
    private static final long RETRY_HEADROOM_MS = 1_000;

    private final LlmProperties props;
    private final AiAuditWriter writer;
    private final PromptBuilder prompts;
    private final OutputValidator validator;
    private final SoclaasClient client;
    private final LlmScheduler scheduler;
    private final UserThrottle throttle;
    private final CircuitBreaker breaker;
    private final BudgetBlock budget;
    private final Backoff backoff;
    private final Sleeper sleeper;
    private final Clock clock;

    /** Creates the gateway over its collaborators; all of them are beans of {@code AiConfig}. */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public LlmGateway(LlmProperties props, AiAuditWriter writer, PromptBuilder prompts, OutputValidator validator,
                      SoclaasClient client, LlmScheduler scheduler, UserThrottle throttle, CircuitBreaker breaker,
                      BudgetBlock budget, Backoff backoff, Sleeper sleeper, Clock clock) {
        this.props = props;
        this.writer = writer;
        this.prompts = prompts;
        this.validator = validator;
        this.client = client;
        this.scheduler = scheduler;
        this.throttle = throttle;
        this.breaker = breaker;
        this.budget = budget;
        this.backoff = backoff;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    /** What happened to one call; mutated by the pipeline and read by {@link #finish}. */
    private static final class Progress {
        private final Instant start;
        private PromptBuilder.Prepared prepared;
        private boolean sent;
        private int attempts;
        private int tokensIn;
        private int tokensOut;
        private int httpStatus;
        private String model;
        private String text;
        private String finishReason;
        private LlmErrorCode lastFailure;
        private LlmErrorCode code;

        Progress(Instant start) {
            this.start = start;
        }
    }

    /**
     * Runs one LLM call.
     *
     * @param feature the registered feature
     * @param call the call, built by role code from its authorized principal
     * @param <T> the feature's output type
     * @return the validated output, or the feature's fallback with a code
     * @throws IllegalArgumentException when {@code feature} or {@code call} is null or the call is malformed
     * @throws IllegalStateException when a database transaction is active
     */
    public <T> LlmResult<T> run(AiFeature<T> feature, LlmCall call) {
        UUID callId = UUID.randomUUID();
        Progress st = new Progress(clock.instant());
        if (feature == null || call == null) {
            throw new IllegalArgumentException("LlmGateway: feature and call must not be null");
        }
        call.checkShape();
        feature.validate(props);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("LlmGateway.run must not be called inside a transaction");
        }
        if (!props.enabled()) {
            return unaudited(feature, call, LlmErrorCode.DISABLED, callId, st);
        }
        if (!feature.allowedRoles().contains(call.role())) {
            return unaudited(feature, call, LlmErrorCode.ROLE_DENIED, callId, st);
        }
        if (!throttle.tryReserveMinute(call.userId(), feature.id(), feature.perUserPerMinute())
            || !throttle.tryEnter(call.userId())) {
            return unaudited(feature, call, LlmErrorCode.USER_THROTTLED, callId, st);
        }
        try {
            return execute(feature, call, callId, st);
        } finally {
            throttle.leave(call.userId());
        }
    }

    private <T> LlmResult<T> execute(AiFeature<T> feature, LlmCall call, UUID callId, Progress st) {
        st.prepared = prompts.build(feature, call);
        if (st.prepared.blocked()) {
            st.code = LlmErrorCode.BLOCKED_INPUT;
            return finish(feature, call, callId, st, false);
        }
        AiAuditWriter.CapResult cap = writer.reserve(call.userId(), feature.priority());
        if (cap == AiAuditWriter.CapResult.COUNT_FAILED) {
            return unaudited(feature, call, LlmErrorCode.AUDIT_FAILED, callId, st);
        }
        if (cap == AiAuditWriter.CapResult.DAILY_CAP) {
            st.code = LlmErrorCode.DAILY_CAP;
            return finish(feature, call, callId, st, false);
        }
        try {
            if (props.mock()) {
                st.sent = true;
                st.model = "mock";
                st.text = feature.mockResponse();
                st.finishReason = "stop";
            } else {
                sendLoop(feature, call, st);
            }
        } catch (RuntimeException e) {
            writer.release(call.userId(), false);
            throw e;
        }
        return finish(feature, call, callId, st, true);
    }

    private void sendLoop(AiFeature<?> feature, LlmCall call, Progress st) {
        Instant deadline = st.start.plusMillis(props.deadlineMs());
        int maxAttempts = props.maxRetries() + 1;
        st.model = modelFor(feature.tier());
        List<SoclaasClient.Message> messages = st.prepared.messages().stream()
            .map(m -> new SoclaasClient.Message(m.role(), m.content())).toList();
        SoclaasClient.Request request = new SoclaasClient.Request(st.model, messages, feature.maxTokens(),
            feature.temperature(), feature.output().isJson());
        for (int n = 0;; n++) {
            Admission b = budget.tryAdmit();
            if (b == Admission.REJECTED) {
                stop(st, LlmErrorCode.BUDGET_EXHAUSTED);
                return;
            }
            Admission c = breaker.tryAdmit();
            if (c == Admission.REJECTED) {
                abandon(b, Admission.ALLOWED);
                stop(st, LlmErrorCode.CIRCUIT_OPEN);
                return;
            }
            long remaining = millisUntil(deadline);
            if (remaining <= 0) {
                abandon(b, c);
                stop(st, LlmErrorCode.QUEUE_TIMEOUT);
                return;
            }
            LlmScheduler.Permit permit;
            try {
                permit = scheduler.acquire(feature.priority(), st.start,
                    Duration.ofMillis(Math.min(props.queueWaitMs(), remaining)));
            } catch (LlmScheduler.SchedulerRejection r) {
                abandon(b, c);
                stop(st, r.reason() == LlmScheduler.Reason.QUEUE_FULL
                    ? LlmErrorCode.QUEUE_FULL : LlmErrorCode.QUEUE_TIMEOUT);
                return;
            }
            AttemptResult result;
            try (permit) {
                long timeout = Math.max(1, Math.min(props.requestTimeoutMs(), millisUntil(deadline)));
                result = client.attempt(request, Duration.ofMillis(timeout));
            }
            st.attempts++;
            st.sent = true;
            if (settle(st, call.userId(), result, b == Admission.PROBE, c == Admission.PROBE)) {
                return;
            }
            LlmErrorCode failure = st.lastFailure;
            boolean probe = b == Admission.PROBE || c == Admission.PROBE;
            if (probe || n + 1 >= maxAttempts || Thread.currentThread().isInterrupted()) {
                st.code = failure;
                return;
            }
            Integer retryAfter = ((AttemptResult.Failure) result).retryAfterSeconds();
            long delay = backoff.delayMs(n, retryAfter == null ? null : retryAfter.longValue());
            if (millisUntil(deadline) < delay + RETRY_HEADROOM_MS) {
                st.code = failure;
                return;
            }
            try {
                sleeper.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                st.code = failure;
                return;
            }
        }
    }

    /**
     * Records an attempt's outcome on the breaker and budget block.
     *
     * @return true when the loop ends here (success, or a failure that is never retried)
     */
    private boolean settle(Progress st, UUID userId, AttemptResult result, boolean budgetProbe,
                           boolean breakerProbe) {
        if (result instanceof AttemptResult.Success s) {
            breaker.recordResponse(breakerProbe);
            budget.recordOtherResponse(budgetProbe);
            st.text = s.content();
            st.finishReason = s.finishReason();
            st.tokensIn = s.tokensIn();
            st.tokensOut = s.tokensOut();
            return true;
        }
        AttemptResult.Failure f = (AttemptResult.Failure) result;
        st.lastFailure = f.code();
        st.httpStatus = f.httpStatus();
        if (f.budget()) {
            budget.recordBudgetExhausted();
            breaker.recordResponse(breakerProbe);
            st.code = LlmErrorCode.BUDGET_EXHAUSTED;
            return true;
        }
        switch (f.code()) {
            case SERVER, TIMEOUT, NETWORK -> {
                breaker.recordFailure(userId, breakerProbe);
                if (budgetProbe) {
                    budget.abandonProbe();
                }
            }
            default -> {
                breaker.recordResponse(breakerProbe);
                budget.recordOtherResponse(budgetProbe);
            }
        }
        boolean retryable = switch (f.code()) {
            case RATE_LIMITED, SERVER, TIMEOUT, NETWORK -> true;
            default -> false;
        };
        if (!retryable) {
            st.code = f.code();
            return true;
        }
        return false;
    }

    private void abandon(Admission budgetAdmission, Admission breakerAdmission) {
        if (budgetAdmission == Admission.PROBE) {
            budget.abandonProbe();
        }
        if (breakerAdmission == Admission.PROBE) {
            breaker.abandonProbe();
        }
    }

    /** Ends the loop on a refusal: before any request was sent it is the refusal's code, after, the last failure. */
    private void stop(Progress st, LlmErrorCode refusal) {
        st.code = st.attempts > 0 && st.lastFailure != null ? st.lastFailure : refusal;
    }

    private <T> LlmResult<T> finish(AiFeature<T> feature, LlmCall call, UUID callId, Progress st, boolean reserved) {
        LlmErrorCode code = st.code;
        T value = null;
        if (code == null) {
            try {
                OutputValidator.Outcome<T> outcome =
                    validator.validate(st.text, st.finishReason, feature.output(), props.apiKey());
                if (outcome.ok()) {
                    value = outcome.value();
                } else {
                    code = outcome.code();
                }
            } catch (RuntimeException e) {
                LOG.warn("AI output validation failed: feature={} class={}", feature.id(), e.getClass().getName());
                code = LlmErrorCode.INVALID_OUTPUT;
            }
        }
        boolean written = writeRow(feature, call, callId, st, code);
        if (reserved) {
            writer.release(call.userId(), st.sent && !written);
        }
        if (!written && code == null) {
            code = LlmErrorCode.AUDIT_FAILED;
            value = null;
        }
        boolean flagged = st.prepared != null && st.prepared.flagged();
        boolean truncated = st.prepared != null && st.prepared.truncated();
        if (code == null) {
            return LlmResult.ok(value, flagged, truncated, callId);
        }
        logFailure(feature, call, st, code);
        return LlmResult.degraded(feature.fallback().apply(call), code, flagged, truncated, callId);
    }

    private boolean writeRow(AiFeature<?> feature, LlmCall call, UUID callId, Progress st, LlmErrorCode code) {
        PromptBuilder.Prepared p = st.prepared;
        AiAuditWriter.Details details = new AiAuditWriter.Details(feature.id(), st.sent ? st.model : null,
            st.tokensIn, st.tokensOut, Duration.between(st.start, clock.instant()).toMillis(), st.attempts,
            p != null && p.flagged(), p != null && p.truncated(), p == null ? null : p.promptHash());
        try {
            boolean written = writer.write(callId, call.userId(), code, st.sent, call.audit(), details);
            if (!written) {
                LOG.warn("AI audit row not written: feature={} user={} code={}", feature.id(), call.userId(), code);
            }
            return written;
        } catch (RuntimeException e) {
            LOG.warn("AI audit write failed: feature={} user={} class={}", feature.id(), call.userId(),
                e.getClass().getName());
            return false;
        }
    }

    private <T> LlmResult<T> unaudited(AiFeature<T> feature, LlmCall call, LlmErrorCode code, UUID callId,
                                       Progress st) {
        logFailure(feature, call, st, code);
        return LlmResult.degraded(feature.fallback().apply(call), code, false, false, callId);
    }

    private void logFailure(AiFeature<?> feature, LlmCall call, Progress st, LlmErrorCode code) {
        long latency = Duration.between(st.start, clock.instant()).toMillis();
        if (code == LlmErrorCode.AUTH) {
            LOG.error("AI call failed: feature={} user={} code={} status={} latencyMs={}", feature.id(),
                call.userId(), code, st.httpStatus, latency);
        } else {
            LOG.warn("AI call degraded: feature={} user={} code={} status={} latencyMs={}", feature.id(),
                call.userId(), code, st.httpStatus, latency);
        }
    }

    private long millisUntil(Instant deadline) {
        return Duration.between(clock.instant(), deadline).toMillis();
    }

    private String modelFor(ModelTier tier) {
        return switch (tier) {
            case FAST -> props.modelFast();
            case STANDARD -> props.modelStandard();
            case STRONG -> props.modelStrong();
        };
    }
}
