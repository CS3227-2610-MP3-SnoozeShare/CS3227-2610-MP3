package com.snoozeshare.service.assistant;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.LlmGateway;
import com.snoozeshare.ai.LlmResult;
import com.snoozeshare.config.ConditionalOnDatabase;

/** Read snapshot, call outside transactions, then atomically apply and catch up after commit. */
@Component
@ConditionalOnDatabase
@ConditionalOnProperty(prefix = "snoozeshare.assistant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AssistantJob {
    private static final Logger LOG = LoggerFactory.getLogger(AssistantJob.class);
    private final AssistantContextBuilder context;
    private final AssistantApplier applier;
    private final AssistantRunner runner;
    private final LlmGateway gateway;
    private final AiFeature<ClarifyOutput> feature;
    private final TransactionTemplate read;

    public AssistantJob(AssistantContextBuilder context, AssistantApplier applier, AssistantRunner runner,
            LlmGateway gateway, AiFeature<ClarifyOutput> feature, PlatformTransactionManager transactions) {
        this.context = context;
        this.applier = applier;
        this.runner = runner;
        this.gateway = gateway;
        this.feature = feature;
        read = new TransactionTemplate(transactions);
        read.setReadOnly(true);
    }

    /** Contains queue rejection and database failures so no listener failure reaches a request. */
    public void submit(UUID id) {
        try {
            runner.submit(() -> run(id));
        } catch (RejectedExecutionException failure) {
            try {
                applier.reject(id);
            } catch (RuntimeException writeFailure) {
                failed(id, writeFailure);
            }
        }
    }

    /** Runs only on a runner thread in production; tests may drive a turn without triggering listeners. */
    public void run(UUID id) {
        try {
            var snapshot = read.execute(status -> context.load(id));
            if (snapshot == null) {
                LOG.info("Assistant ticket={} outcome=missing", id);
                return;
            }
            if (!snapshot.open() || !snapshot.pending()) {
                return;
            }
            if (!snapshot.guestRaised()) {
                applier.preflight(id, "delete");
                return;
            }
            if (!snapshot.activeGuest() || snapshot.agentSpoke()) {
                applier.preflight(id, "stop");
                return;
            }
            if (!snapshot.userLast()) {
                applier.preflight(id, "wait");
                return;
            }
            LlmResult<ClarifyOutput> result;
            long started = System.nanoTime();
            try {
                result = gateway.run(feature, snapshot.call());
            } catch (RuntimeException failure) {
                failed(id, failure);
                result = LlmResult.degraded(ClarifyOutput.NONE, LlmErrorCode.NETWORK, false, false, UUID.randomUUID());
            }
            boolean waiting = applier.apply(id, snapshot.lastGuestMessageId(), result);
            LOG.info("Assistant feature={} ticket={} code={} latencyMs={} waiting={}", feature.id(), id,
                result.code(), (System.nanoTime() - started) / 1_000_000, waiting);
            if (waiting && Boolean.TRUE.equals(read.execute(status -> context.hasNewerGuest(id,
                    snapshot.lastGuestMessageId()))) && applier.claimWaiting(id)) {
                submit(id);
            }
        } catch (RuntimeException failure) {
            failed(id, failure);
        }
    }

    private void failed(UUID id, RuntimeException failure) {
        LOG.warn("Assistant ticket={} failure={}", id, failure.getClass().getSimpleName());
    }
}
