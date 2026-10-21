package com.snoozeshare.auth.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationException;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.VerificationFailure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Establishes a stateless Spring Security principal from a Firebase bearer token. */
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private final FirebaseTokenVerifier verifier;

    public FirebaseAuthFilter(FirebaseTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !isProtectedPath(path) || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        String token = bearerToken(request.getHeader("Authorization"));
        if (token == null) {
            AuthErrorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
            return;
        }
        try {
            FirebaseAuthentication identity = verifier.verify(token);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(identity, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (VerificationException exception) {
            int status = exception.failure() == VerificationFailure.PROVIDER_UNAVAILABLE
                ? HttpServletResponse.SC_SERVICE_UNAVAILABLE
                : HttpServletResponse.SC_UNAUTHORIZED;
            String code = exception.failure() == VerificationFailure.PROVIDER_UNAVAILABLE
                ? "AUTH_PROVIDER_UNAVAILABLE" : "INVALID_TOKEN";
            AuthErrorWriter.write(request, response, status, code);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isProtectedPath(String path) {
        return "/api/auth/register".equals(path)
            || "/api/auth/me".equals(path)
            || path.startsWith("/api/guest/")
            || path.startsWith("/api/host/")
            || path.startsWith("/api/agent/");
    }

    private String bearerToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }
}
