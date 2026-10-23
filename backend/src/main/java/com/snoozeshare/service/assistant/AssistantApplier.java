package com.snoozeshare.service.assistant;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.LlmResult;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;
import com.snoozeshare.domain.model.Ticket;
import com.snoozeshare.domain.model.TicketAssistant;
import com.snoozeshare.domain.model.TicketAssistant.State;
import com.snoozeshare.repository.MessageRepository;
import com.snoozeshare.repository.RowLocks;
import com.snoozeshare.repository.TicketAssistantRepository;
import com.snoozeshare.repository.TicketRepository;
import com.snoozeshare.service.AuditRecord;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.MessageService;

/** Every entry commits independently; all result writes follow ticket then assistant lock order. */
@Component
@ConditionalOnDatabase
@ConditionalOnProperty(prefix = "snoozeshare.assistant", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AssistantApplier {
    private static final Logger LOG = LoggerFactory.getLogger(AssistantApplier.class);
    private final TicketRepository tickets;
    private final TicketAssistantRepository assistants;
    private final MessageRepository messages;
    private final MessageService posting;
    private final AuditService audit;
    private final RowLocks locks;
    private final AssistantProperties properties;
    private final Clock clock;
    private final TransactionTemplate write;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public AssistantApplier(TicketRepository tickets, TicketAssistantRepository assistants, MessageRepository messages,
            MessageService posting, AuditService audit, RowLocks locks, AssistantProperties properties, Clock clock,
            PlatformTransactionManager transactions) {
        this.tickets = tickets;
        this.assistants = assistants;
        this.messages = messages;
        this.posting = posting;
        this.audit = audit;
        this.locks = locks;
        this.properties = properties;
        this.clock = clock;
        write = new TransactionTemplate(transactions);
        write.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public boolean insertPending(UUID id) {
        return Boolean.TRUE.equals(write.execute(status -> assistants.insertPending(id, clock.instant()) == 1));
    }

    /** Assistant-only compare-and-set: no ticket row lock is taken by a reply listener. */
    public boolean claimWaiting(UUID id) {
        return Boolean.TRUE.equals(write.execute(status -> {
            locks.timeout();
            return assistants.claimWaiting(id, clock.instant()) == 1;
        }));
    }

    /** Agent listener path takes only the assistant lock, avoiding inversion with Agent mutations. */
    public void stop(UUID id) {
        write.executeWithoutResult(status -> {
            locks.timeout();
            TicketAssistant row = assistants.findByTicketIdForUpdate(id).orElse(null);
            if (row != null && (row.getState() == State.PENDING || row.getState() == State.WAITING_GUEST)) {
                Ticket ticket = tickets.findById(id).orElse(null);
                if (ticket != null) {
                    transition(ticket, row, State.STOPPED);
                }
            }
        });
    }

    /** Preflight actions recheck the same locked lifecycle guards as result application. */
    public void preflight(UUID id, String action) {
        write.executeWithoutResult(status -> {
            locks.timeout();
            Ticket ticket = tickets.findByIdForUpdate(id).orElse(null);
            TicketAssistant row = assistants.findByTicketIdForUpdate(id).orElse(null);
            if (!pending(ticket, row)) {
                return;
            }
            if ("delete".equals(action)) {
                assistants.delete(row);
            } else if ("stop".equals(action)) {
                transition(ticket, row, State.STOPPED);
            } else if ("wait".equals(action)) {
                row.setState(State.WAITING_GUEST);
                row.setUpdatedAt(clock.instant());
            } else {
                throw new IllegalArgumentException("Unknown preflight action");
            }
        });
    }

    public void reject(UUID id) {
        apply(id, null, LlmResult.degraded(ClarifyOutput.NONE, LlmErrorCode.QUEUE_FULL,
            false, false, UUID.randomUUID()), false);
    }

    /** Returns true only after an ordinary question round has committed in WAITING_GUEST. */
    public boolean apply(UUID id, UUID lastGuest, LlmResult<ClarifyOutput> result) {
        return apply(id, lastGuest, result, true);
    }

    private boolean apply(UUID id, UUID lastGuest, LlmResult<ClarifyOutput> result, boolean updateCursor) {
        return Boolean.TRUE.equals(write.execute(status -> {
            locks.timeout();
            Ticket ticket = tickets.findByIdForUpdate(id).orElse(null);
            TicketAssistant row = assistants.findByTicketIdForUpdate(id).orElse(null);
            if (!pending(ticket, row)) {
                return false;
            }
            if (messages.findThread(id, ThreadChannel.GUEST).stream().anyMatch(m -> m.getAuthorRole() == Role.AGENT)) {
                transition(ticket, row, State.STOPPED);
                return false;
            }
            if (!result.ok() && result.code() == LlmErrorCode.DISABLED && row.getRounds() == 0
                    && row.getDraft() == null) {
                assistants.delete(row);
                return false;
            }
            if (updateCursor) {
                row.setLastMessageId(lastGuest);
            }
            if (!result.ok()) {
                if (row.getRounds() >= 1) {
                    post(ticket, ClarifyRendering.CLOSING, "closing");
                }
                transition(ticket, row, State.UNAVAILABLE);
                return false;
            }
            ClarifyOutput output = result.value();
            List<String> questions = ClarifySafety.questions(output.questions());
            row.setFlagged(row.isFlagged() || result.flagged());
            row.setDraft(ClarifyRendering.draft(output, row.isFlagged(), result.truncated()));
            row.setDraftUpdatedAt(clock.instant());
            if (row.isFlagged() || Boolean.TRUE.equals(output.enough()) || questions.isEmpty()
                    || row.getRounds() >= properties.maxRounds()) {
                if (row.getRounds() >= 1) {
                    post(ticket, ClarifyRendering.CLOSING, "closing");
                }
                transition(ticket, row, State.DONE);
                return false;
            }
            post(ticket, ClarifyRendering.questions(questions, ticket.getId()), "question");
            row.setRounds((short) (row.getRounds() + 1));
            transition(ticket, row, State.WAITING_GUEST);
            return true;
        }));
    }

    private boolean pending(Ticket ticket, TicketAssistant row) {
        return ticket != null && AssistantContextBuilder.open(ticket)
            && row != null && row.getState() == State.PENDING;
    }

    private void transition(Ticket ticket, TicketAssistant row, State next) {
        audit.record(base(ticket, AuditAction.ASSISTANT_STATE, "Ticket", ticket.getId())
            .status(row.getState(), next).build());
        LOG.info("Assistant ticket={} before={} after={} rounds={}",
            ticket.getId(), row.getState(), next, row.getRounds());
        row.setState(next);
        row.setUpdatedAt(clock.instant());
    }

    private void post(Ticket ticket, String body, String kind) {
        Message message = posting.postAssistant(ticket.getId(), body);
        audit.record(base(ticket, AuditAction.ASSISTANT_MESSAGE, "Message", message.getId())
            .status(null, kind).build());
    }

    private AuditRecord.Builder base(Ticket ticket, AuditAction action, String entity, UUID entityId) {
        return AuditRecord.builder(AuditService.SYSTEM_ACTOR_ID, action, entity, entityId)
            .subject(ticket.getRaisedByUserId()).booking(ticket.getBookingId())
            .ticket(ticket.getId()).at(clock.instant());
    }
}
