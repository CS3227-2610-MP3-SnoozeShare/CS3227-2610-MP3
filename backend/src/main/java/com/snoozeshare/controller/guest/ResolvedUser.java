package com.snoozeshare.controller.guest;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Reads the authenticated user ID set by {@code RoleAuthorizationFilter}.
 */
public final class ResolvedUser {

    private ResolvedUser() {
    }

    public static UUID id(HttpServletRequest request) {
        Object attribute = request.getAttribute("snoozeshare.userId");
        if (attribute instanceof UUID userId) {
            return userId;
        }
        throw new IllegalStateException("No authenticated user ID on request");
    }
}
