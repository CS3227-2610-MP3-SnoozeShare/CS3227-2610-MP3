package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

public record BookingCancelledEvent(
        UUID bookingId, UUID actorId, Instant occurredAt) {
}
