package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

public record MessagePostedEvent(
        UUID messageId, UUID ticketId, Instant occurredAt) {
}
