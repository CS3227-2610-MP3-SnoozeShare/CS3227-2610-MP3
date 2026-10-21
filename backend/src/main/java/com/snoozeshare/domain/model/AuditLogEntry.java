package com.snoozeshare.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An immutable audit-log row. Status rows track state changes; money rows track wallet movements. */
@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;

    @Column(name = "wallet_adjustment", precision = 19, scale = 4)
    private BigDecimal walletAdjustment;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "subject_user_id")
    private UUID subjectUserId;

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    /** JPA requires a no-arg constructor. */
    protected AuditLogEntry() {}

    /** Creates a new audit log entry. */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public AuditLogEntry(UUID id, UUID actorUserId,
                         String actorName, String actionType,
                         String entityType, UUID entityId,
                         String beforeState, String afterState,
                         BigDecimal walletAdjustment, String reason,
                         UUID subjectUserId, String subjectName,
                         UUID bookingId, UUID ticketId,
                         Instant timestamp,
                         BigDecimal balanceAfter) {
        this.id = id;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.walletAdjustment = walletAdjustment;
        this.reason = reason;
        this.subjectUserId = subjectUserId;
        this.subjectName = subjectName;
        this.bookingId = bookingId;
        this.ticketId = ticketId;
        this.timestamp = timestamp;
        this.balanceAfter = balanceAfter;
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getActionType() {
        return actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getBeforeState() {
        return beforeState;
    }

    public String getAfterState() {
        return afterState;
    }

    public BigDecimal getWalletAdjustment() {
        return walletAdjustment;
    }

    public String getReason() {
        return reason;
    }

    public UUID getSubjectUserId() {
        return subjectUserId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }
}
