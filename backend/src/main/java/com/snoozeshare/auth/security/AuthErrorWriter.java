package com.snoozeshare.auth.security;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Writes the stable, non-secret JSON error shape used at the security boundary. */
final class AuthErrorWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthErrorWriter.class);

    private AuthErrorWriter() { }

    static void write(
        HttpServletRequest request,
        HttpServletResponse response,
        int status,
        String code) throws IOException {
        String requestId = UUID.randomUUID().toString();
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"code\":\"" + code + "\",\"message\":\"Request cannot be authorized\","
                + "\"requestId\":\"" + requestId + "\"}");
        LOGGER.warn("Auth request failed requestId={} status={} code={} path={}",
            requestId, status, code, request.getRequestURI());
    }
}
