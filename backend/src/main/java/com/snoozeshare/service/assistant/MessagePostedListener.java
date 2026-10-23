package com.snoozeshare.service.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.infra.events.MessagePostedEvent;
import com.snoozeshare.repository.MessageRepository;

/**
 * Decides from the stored message only: a Guest reply wakes a waiting assistant, an Agent post in the Guest channel
 * stops it. The event is already published after commit, so this is a plain listener.
 */
@Component
@ConditionalOnDatabase
@ConditionalOnProperty(prefix = "snoozeshare.assistant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MessagePostedListener {
    private static final Logger LOG = LoggerFactory.getLogger(MessagePostedListener.class);
    private final MessageRepository messages;
    private final AssistantApplier applier;
    private final AssistantJob job;

    public MessagePostedListener(MessageRepository messages, AssistantApplier applier, AssistantJob job) {
        this.messages = messages;
        this.applier = applier;
        this.job = job;
    }

    @EventListener
    public void onMessagePosted(MessagePostedEvent event) {
        try {
            Message message = messages.findById(event.messageId()).orElse(null);
            if (message == null || message.getChannel() != ThreadChannel.GUEST) {
                return;
            }
            Role author = message.getAuthorRole();
            if (author == Role.GUEST) {
                if (applier.claimWaiting(message.getTicketId())) {
                    job.submit(message.getTicketId());
                    LOG.info("Assistant ticket={} role=GUEST decision=resumed", message.getTicketId());
                }
            } else if (author == Role.AGENT) {
                applier.stop(message.getTicketId());
                LOG.info("Assistant ticket={} role=AGENT decision=stop", message.getTicketId());
            }
        } catch (RuntimeException failure) {
            LOG.warn("Assistant ticket={} listener failure={}", event.ticketId(), failure.getClass().getSimpleName());
        }
    }
}
