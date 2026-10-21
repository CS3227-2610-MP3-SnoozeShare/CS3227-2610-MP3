package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

public record TicketResolvedEvent(
        UUID ticketId, UUID actorId, Instant occurredAt) {
}
