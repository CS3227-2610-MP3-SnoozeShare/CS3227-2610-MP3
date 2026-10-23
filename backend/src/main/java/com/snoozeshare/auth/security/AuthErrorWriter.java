package com.snoozeshare.auth.security;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Writes the stable, non-secret JSON error shape used at the security boundary. */
public final class AuthErrorWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthErrorWriter.class);

    private AuthErrorWriter() { }

    public static void write(
        HttpServletRequest request,
        HttpServletResponse response,
        int status,
        String code) throws IOException {
        write(request, response, status, code, "Request cannot be authorized");
    }

    public static void write(
        HttpServletRequest request,
        HttpServletResponse response,
        int status,
        String code,
        String message) throws IOException {
        String requestId = UUID.randomUUID().toString();
        response.setStatus(status);
        response.setContentType("application/json");
        if (isAgentPath(ProtectedRequestPath.withinApplication(request))) {
            response.setHeader("Cache-Control", "no-store");
        }
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\""
            + message + "\",\"requestId\":\"" + requestId + "\"}");
        LOGGER.warn("Auth request failed requestId={} status={} code={} path={}",
            requestId, status, code, request.getRequestURI());
    }

    private static boolean isAgentPath(String path) {
        return "/api/agent".equals(path) || path.startsWith("/api/agent/");
    }
}
