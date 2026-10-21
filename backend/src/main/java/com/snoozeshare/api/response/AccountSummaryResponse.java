package com.snoozeshare.api.response;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.service.AccountSummary;

public record AccountSummaryResponse(
        UUID userId,
        String displayName,
        String email,
        Role role,
        Instant createdAt,
        AccountStatus status,
        String suspensionReason) {

    public static AccountSummaryResponse from(AccountSummary a) {
        return new AccountSummaryResponse(a.userId(), a.displayName(), a.email(),
                a.role(), a.createdAt(), a.status(), a.suspensionReason());
    }
}
