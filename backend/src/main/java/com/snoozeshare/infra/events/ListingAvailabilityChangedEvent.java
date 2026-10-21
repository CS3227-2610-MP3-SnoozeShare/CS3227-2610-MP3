package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

public record ListingAvailabilityChangedEvent(
        UUID propertyId, UUID actorId, Instant occurredAt) {
}
