package com.snoozeshare.ai.audit;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.snoozeshare.ai.AuditContext;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.config.LlmProperties;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.repository.AuditLogRepository;
import com.snoozeshare.service.AuditRecord;
import com.snoozeshare.service.AuditService;

/**
 * Writes the one audit row of an AI call and enforces the daily caps over those rows.
 *
 * <p>A cap is the stored {@code AI_CALL} rows of the UTC day plus the calls reserved but not yet written, read and
 * reserved under one lock, so a parallel burst cannot pass a cap. Rows carry metadata only, never prompt, reply or
 * upstream error text.
 */
@Component
@ConditionalOnDatabase
public class AiAuditWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AiAuditWriter.class);
    private static final String ENTITY_TYPE = "AI_CALL";
    private static final String NONE = "-";
    private static final long COALESCE_MINUTES = 10;

    /** The outcome of {@link #reserve}. */
    public enum CapResult {
        /** One call is reserved; the caller must release it. */
        RESERVED,
        /** The per-user or global cap is used up; nothing is reserved. */
        DAILY_CAP,
        /** A count query failed; nothing is reserved and no request may be sent. */
        COUNT_FAILED
    }

    /**
     * The metadata of one call, written as the row's after-state line.
     *
     * @param feature the feature id
     * @param model the model name, {@code mock} in mock mode, or null when no request was sent
     * @param tokensIn prompt tokens reported by the service, 0 when unknown
     * @param tokensOut completion tokens reported by the service, 0 when unknown
     * @param latencyMs milliseconds from entry to outcome
     * @param attempts requests sent
     * @param flagged whether the injection detector matched
     * @param truncated whether any input was cut
     * @param promptHash SHA-256 hex of the assembled messages, or null when the prompt was blocked
     */
    public record Details(String feature, String model, int tokensIn, int tokensOut, long latencyMs, int attempts,
            boolean flagged, boolean truncated, String promptHash) {

        /** The metadata line of the data model, fixed order, separator {@code ;}. */
        String line() {
            return "feature=" + feature + ";model=" + (model == null || model.isBlank() ? NONE : model)
                    + ";tokensIn=" + tokensIn + ";tokensOut=" + tokensOut + ";latencyMs=" + latencyMs
                    + ";attempts=" + attempts + ";flagged=" + flagged + ";truncated=" + truncated
                    + ";promptHash=" + (promptHash == null || promptHash.isBlank() ? NONE : promptHash);
        }
    }

    private final AuditService audit;
    private final AuditLogRepository entries;
    private final LlmProperties props;
    private final Clock clock;

    private final Object lock = new Object();
    private final Map<UUID, Integer> reservedByUser = new HashMap<>();
    private final Map<UUID, Integer> pinnedByUser = new HashMap<>();
    private int reservedGlobal;
    private int pinnedGlobal;
    private LocalDate pinnedDay;

    private final Object coalesceLock = new Object();
    private final Map<String, Instant> lastRejectedRow = new HashMap<>();

    /** Creates the writer over the shared audit service and repository. */
    public AiAuditWriter(AuditService audit, AuditLogRepository entries, LlmProperties props, Clock clock) {
        this.audit = audit;
        this.entries = entries;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Checks both daily caps and, when both pass, reserves one call. Reservations are read before the rows are
     * counted, all under one lock. A priority 0 call may use the whole global cap; other priorities stop short of
     * the share held back for priority 0.
     */
    public CapResult reserve(UUID userId, int priority) {
        synchronized (lock) {
            LocalDate today = rollPinnedDay();
            int reservedUser = reservedByUser.getOrDefault(userId, 0) + pinnedByUser.getOrDefault(userId, 0);
            int reservedAll = reservedGlobal + pinnedGlobal;
            try {
                Instant dayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant();
                long userRows = entries.countByActorUserIdAndActionTypeAndTimestampGreaterThanEqual(
                        userId, AuditAction.AI_CALL.name(), dayStart);
                long allRows = entries.countByActionTypeAndTimestampGreaterThanEqual(
                        AuditAction.AI_CALL.name(), dayStart);
                long globalLimit = priority == 0
                        ? props.globalDailyCap()
                        : (long) props.globalDailyCap() * (100 - props.priorityZeroReservePercent()) / 100;
                if (userRows + reservedUser >= props.dailyCapPerUser() || allRows + reservedAll >= globalLimit) {
                    return CapResult.DAILY_CAP;
                }
            } catch (RuntimeException e) {
                LOG.warn("AI cap count failed: {}", e.getClass().getSimpleName());
                return CapResult.COUNT_FAILED;
            }
            reservedByUser.merge(userId, 1, Integer::sum);
            reservedGlobal++;
            return CapResult.RESERVED;
        }
    }

    /**
     * Releases a reservation taken by {@link #reserve}, after the row is written. With {@code keepCounted} (a sent
     * call whose row could not be written) the call stays counted until 00:00 UTC.
     */
    public void release(UUID userId, boolean keepCounted) {
        synchronized (lock) {
            rollPinnedDay();
            reservedByUser.computeIfPresent(userId, (id, n) -> n > 1 ? n - 1 : null);
            if (reservedGlobal > 0) {
                reservedGlobal--;
            }
            if (keepCounted) {
                pinnedByUser.merge(userId, 1, Integer::sum);
                pinnedGlobal++;
            }
        }
    }

    /**
     * Writes the audit row of a call.
     *
     * @param callId the call id, stored as the entity id
     * @param userId the calling user
     * @param code the error code, or null for an ok outcome
     * @param asCall true for an {@code AI_CALL} row (a request was sent, or mock mode), false for a rejected row
     * @param context ticket, booking and subject references from the caller
     * @param details the metadata line
     * @return false when the row could not be written; true when written or skipped by the 10-minute coalescing of
     *         rejected rows
     */
    public boolean write(UUID callId, UUID userId, LlmErrorCode code, boolean asCall, AuditContext context,
            Details details) {
        String reason = code == null ? "OK" : code.name();
        String claim = null;
        if (!asCall) {
            claim = claimRejectedSlot(userId, reason);
            if (claim == null) {
                LOG.info("AI rejected row coalesced: feature={} user={} code={}", details.feature(), userId, reason);
                return true;
            }
        }
        AuditContext ctx = context == null ? AuditContext.NONE : context;
        try {
            audit.record(AuditRecord.builder(userId, asCall ? AuditAction.AI_CALL : AuditAction.AI_CALL_REJECTED,
                    ENTITY_TYPE, callId)
                    .status(null, details.line())
                    .reason(reason)
                    .subject(ctx.subjectUserId())
                    .booking(ctx.bookingId())
                    .ticket(ctx.ticketId())
                    .at(clock.instant())
                    .build());
            return true;
        } catch (RuntimeException e) {
            if (claim != null) {
                synchronized (coalesceLock) {
                    lastRejectedRow.remove(claim);
                }
            }
            LOG.warn("AI audit write failed: feature={} user={} code={} error={}", details.feature(), userId, reason,
                    e.getClass().getSimpleName());
            return false;
        }
    }

    /** Takes the right to write a rejected row for this user and code, or null when one was written lately. */
    private String claimRejectedSlot(UUID userId, String reason) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(COALESCE_MINUTES, ChronoUnit.MINUTES);
        String key = userId + "|" + reason;
        synchronized (coalesceLock) {
            lastRejectedRow.values().removeIf(t -> !t.isAfter(cutoff));
            if (lastRejectedRow.containsKey(key)) {
                return null;
            }
            lastRejectedRow.put(key, now);
            return key;
        }
    }

    /** Clears the failed-audit counts when the UTC day has changed; the caller holds the lock. */
    private LocalDate rollPinnedDay() {
        LocalDate today = clock.instant().atZone(ZoneOffset.UTC).toLocalDate();
        if (!today.equals(pinnedDay)) {
            pinnedByUser.clear();
            pinnedGlobal = 0;
            pinnedDay = today;
        }
        return today;
    }
}
