package com.snoozeshare.domain.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Private draft and bounded conversation state for one Guest ticket. */
@Entity
@Table(name = "ticket_assistants")
public class TicketAssistant {

    /** Terminal states are immutable; PENDING and WAITING_GUEST are the only live states. */
    public enum State {
        PENDING, WAITING_GUEST, DONE, STOPPED, UNAVAILABLE
    }

    @Id
    @Column(name = "ticket_id")
    private UUID ticketId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private State state;
    @Column(nullable = false)
    private short rounds;
    @Column(nullable = false)
    private boolean flagged;
    @Column(columnDefinition = "TEXT")
    private String draft;
    @Column(name = "draft_updated_at")
    private Instant draftUpdatedAt;
    @Column(name = "last_message_id")
    private UUID lastMessageId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TicketAssistant() { }

    /** Creates the initial bookkeeping row before a first call is submitted. */
    public TicketAssistant(UUID ticketId, Instant now) {
        this.ticketId = ticketId;
        state = State.PENDING;
        createdAt = now;
        updatedAt = now;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public short getRounds() {
        return rounds;
    }

    public void setRounds(short rounds) {
        this.rounds = rounds;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
    }

    public Instant getDraftUpdatedAt() {
        return draftUpdatedAt;
    }

    public void setDraftUpdatedAt(Instant draftUpdatedAt) {
        this.draftUpdatedAt = draftUpdatedAt;
    }

    public UUID getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(UUID lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
