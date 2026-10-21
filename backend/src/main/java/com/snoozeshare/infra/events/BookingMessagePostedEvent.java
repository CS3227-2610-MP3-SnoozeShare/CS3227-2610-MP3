package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

public record BookingMessagePostedEvent(
        UUID messageId, UUID bookingId, Instant occurredAt) {
}
