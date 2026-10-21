package com.snoozeshare.infra.events;

import java.time.Instant;
import java.util.UUID;

public record WalletTransactionRecordedEvent(
        UUID transactionId, UUID walletId, Instant occurredAt) {
}
