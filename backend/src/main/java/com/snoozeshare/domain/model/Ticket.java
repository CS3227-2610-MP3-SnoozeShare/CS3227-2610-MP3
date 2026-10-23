package com.snoozeshare.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.RemedyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.TicketStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A dispute ticket raised by a guest or host. */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "raised_by_user_id", nullable = false)
    private UUID raisedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "raised_by_role", nullable = false)
    private Role raisedByRole;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_remedy", nullable = false)
    private RemedyType requestedRemedy;

    @Column(name = "supporting_text", columnDefinition = "TEXT")
    private String supportingText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(name = "agent_notes", columnDefinition = "TEXT")
    private String agentNotes;

    @Column(name = "resolution_reason", columnDefinition = "TEXT")
    private String resolutionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    /** JPA requires a no-arg constructor. */
    protected Ticket() {}

    /** Creates a new ticket. */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public Ticket(UUID id, UUID bookingId,
                  UUID raisedByUserId, Role raisedByRole,
                  String category, String title,
                  String description, RemedyType requestedRemedy,
                  String supportingText, TicketStatus status,
                  UUID assignedAgentId, String agentNotes,
                  String resolutionReason, Instant createdAt,
                  Instant resolvedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.raisedByUserId = raisedByUserId;
        this.raisedByRole = raisedByRole;
        this.category = category;
        this.title = title;
        this.description = description;
        this.requestedRemedy = requestedRemedy;
        this.supportingText = supportingText;
        this.status = status;
        this.assignedAgentId = assignedAgentId;
        this.agentNotes = agentNotes;
        this.resolutionReason = resolutionReason;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getRaisedByUserId() {
        return raisedByUserId;
    }

    public Role getRaisedByRole() {
        return raisedByRole;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public RemedyType getRequestedRemedy() {
        return requestedRemedy;
    }

    public String getSupportingText() {
        return supportingText;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public UUID getAssignedAgentId() {
        return assignedAgentId;
    }

    public void setAssignedAgentId(UUID assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }

    public String getAgentNotes() {
        return agentNotes;
    }

    public void setAgentNotes(String agentNotes) {
        this.agentNotes = agentNotes;
    }

    public String getResolutionReason() {
        return resolutionReason;
    }

    public void setResolutionReason(String resolutionReason) {
        this.resolutionReason = resolutionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
