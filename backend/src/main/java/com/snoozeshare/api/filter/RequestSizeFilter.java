package com.snoozeshare.api.filter;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.snoozeshare.auth.security.AuthErrorWriter;
import com.snoozeshare.auth.security.ProtectedRequestPath;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Enforces the agent API request body size before the body is read. */
public class RequestSizeFilter extends OncePerRequestFilter {

    private static final long MAX_CONTENT_LENGTH = 65_536L;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = agentPath(request);
        return !"/api/agent".equals(path) && !path.startsWith("/api/agent/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength > MAX_CONTENT_LENGTH) {
            AuthErrorWriter.write(request, response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                    "PAYLOAD_TOO_LARGE", "Request is too large");
            return;
        }
        if (contentLength < 0 && request.getHeader("Transfer-Encoding") != null) {
            AuthErrorWriter.write(request, response, HttpServletResponse.SC_LENGTH_REQUIRED,
                    "LENGTH_REQUIRED", "Content-Length is required");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String agentPath(HttpServletRequest request) {
        return ProtectedRequestPath.withinApplication(request);
    }
}
