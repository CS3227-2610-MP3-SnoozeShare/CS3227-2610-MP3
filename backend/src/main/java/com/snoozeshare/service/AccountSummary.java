package com.snoozeshare.service;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;

public record AccountSummary(UUID userId, String displayName, String email, Role role, Instant createdAt,
                             AccountStatus status, String suspensionReason) {

    public static AccountSummary from(User user) {
        return new AccountSummary(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole(),
                user.getCreatedAt(), user.getStatus(), user.getSuspensionReason());
    }

    public boolean governable() {
        return role != Role.AGENT && role != Role.SYSTEM;
    }
}
