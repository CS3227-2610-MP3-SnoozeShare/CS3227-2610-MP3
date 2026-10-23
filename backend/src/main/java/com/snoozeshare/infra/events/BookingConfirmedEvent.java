package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

public record BookingConfirmedEvent(
        UUID bookingId, UUID actorId, Instant occurredAt) {
}
