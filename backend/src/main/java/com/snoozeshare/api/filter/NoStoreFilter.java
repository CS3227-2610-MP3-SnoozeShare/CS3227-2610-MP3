package com.snoozeshare.api.filter;

import java.io.IOException;

import org.springframework.web.filter.OncePerRequestFilter;

import com.snoozeshare.auth.security.ProtectedRequestPath;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Prevents agent API responses from being stored by browsers or intermediaries. */
public class NoStoreFilter extends OncePerRequestFilter {

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
        response.setHeader("Cache-Control", "no-store");
        filterChain.doFilter(request, response);
    }

    private static String agentPath(HttpServletRequest request) {
        return ProtectedRequestPath.withinApplication(request);
    }
}
