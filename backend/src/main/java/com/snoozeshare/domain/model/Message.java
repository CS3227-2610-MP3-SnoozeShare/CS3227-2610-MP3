package com.snoozeshare.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A message in a ticket's chat thread. */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreadChannel channel;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false)
    private Role authorRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    /** JPA requires a no-arg constructor. */
    protected Message() {}

    /** Creates a new message. */
    public Message(UUID id, UUID ticketId,
                   ThreadChannel channel, UUID authorId,
                   Role authorRole, String body,
                   Instant sentAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.channel = channel;
        this.authorId = authorId;
        this.authorRole = authorRole;
        this.body = body;
        this.sentAt = sentAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public ThreadChannel getChannel() {
        return channel;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public Role getAuthorRole() {
        return authorRole;
    }

    public String getBody() {
        return body;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
