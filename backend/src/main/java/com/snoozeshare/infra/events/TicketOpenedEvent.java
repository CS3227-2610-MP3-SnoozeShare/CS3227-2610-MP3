package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

public record TicketOpenedEvent(
        UUID ticketId, UUID actorId, Instant occurredAt) {
}
