package com.snoozeshare.auth.security;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;

/** Writes the stable, non-secret JSON error shape used at the security boundary. */
final class AuthErrorWriter {

    private AuthErrorWriter() { }

    static void write(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"code\":\"" + code + "\",\"message\":\"Request cannot be authorized\","
                + "\"requestId\":\"" + UUID.randomUUID() + "\"}");
    }
}
