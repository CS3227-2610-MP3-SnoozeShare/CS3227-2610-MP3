package com.snoozeshare.auth.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.snoozeshare.api.CurrentUserIdResolver;
import com.snoozeshare.auth.model.AuthRole;
import com.snoozeshare.auth.repository.AuthUserRepository;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Enforces the database role for each role-specific URL namespace. */
public class RoleAuthorizationFilter extends OncePerRequestFilter {

    private final RoleAuthorizationService authorization;

    public RoleAuthorizationFilter(RoleAuthorizationService authorization) {
        this.authorization = authorization;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = protectedPath(request);
        return !path.startsWith("/api/guest/")
            && !path.startsWith("/api/host/")
            && !"/api/agent".equals(path)
            && !path.startsWith("/api/agent/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof FirebaseAuthentication identity)) {
            AuthErrorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN");
            return;
        }
        try {
            AuthUserRepository.AuthUser user = authorization.requireActiveUser(identity);
            authorization.requireRole(user.role(), expectedRole(protectedPath(request)));
            request.setAttribute(CurrentUserIdResolver.ATTRIBUTE, user.id());
            request.setAttribute("snoozeshare.userId", user.id());
            var authenticated = UsernamePasswordAuthenticationToken.authenticated(
                identity,
                null,
                java.util.List.of(() -> "ROLE_" + user.role().name()));
            SecurityContextHolder.getContext().setAuthentication(authenticated);
            filterChain.doFilter(request, response);
        } catch (RoleAuthorizationService.AuthorizationException exception) {
            int status = "PROFILE_NOT_REGISTERED".equals(exception.code())
                ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_FORBIDDEN;
            AuthErrorWriter.write(request, response, status, exception.code());
        }
    }

    private static String protectedPath(HttpServletRequest request) {
        return ProtectedRequestPath.withinApplication(request);
    }

    private AuthRole expectedRole(String path) {
        if (path.startsWith("/api/guest/")) {
            return AuthRole.GUEST;
        }
        if (path.startsWith("/api/host/")) {
            return AuthRole.HOST;
        }
        return AuthRole.AGENT;
    }
}
