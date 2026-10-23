package com.snoozeshare.api.response;

import java.time.Instant;
import java.util.UUID;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.User;

public record UserResponse(
        UUID id,
        String displayName,
        String email,
        Role role,
        AccountStatus status,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail(),
                user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
