package com.snoozeshare.service.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.infra.events.TicketOpenedEvent;

/** Starts the assistant for a committed ticket; nothing here may reach the filing request. */
@Component
@ConditionalOnDatabase
@ConditionalOnProperty(prefix = "snoozeshare.assistant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TicketOpenedListener {
    private static final Logger LOG = LoggerFactory.getLogger(TicketOpenedListener.class);
    private final AssistantApplier applier;
    private final AssistantJob job;

    public TicketOpenedListener(AssistantApplier applier, AssistantJob job) {
        this.applier = applier;
        this.job = job;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketOpened(TicketOpenedEvent event) {
        try {
            if (applier.insertPending(event.ticketId())) {
                job.submit(event.ticketId());
                LOG.info("Assistant ticket={} outcome=submitted", event.ticketId());
            } else {
                LOG.info("Assistant ticket={} outcome=duplicate", event.ticketId());
            }
        } catch (RuntimeException failure) {
            LOG.warn("Assistant ticket={} outcome=failed error={}", event.ticketId(),
                failure.getClass().getSimpleName());
        }
    }
}
