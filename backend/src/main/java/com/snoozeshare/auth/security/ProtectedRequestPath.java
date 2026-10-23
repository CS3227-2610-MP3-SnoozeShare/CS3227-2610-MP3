package com.snoozeshare.auth.security;

import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/** Resolves a decoded, parameter-free application path for protected namespace checks. */
public final class ProtectedRequestPath {

    private ProtectedRequestPath() { }

    public static String withinApplication(HttpServletRequest request) {
        String normalizedUri = request.getRequestURI().replaceAll("/{2,}", "/");
        HttpServletRequest normalizedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                return normalizedUri;
            }
        };
        return UrlPathHelper.defaultInstance.getPathWithinApplication(normalizedRequest);
    }
}
