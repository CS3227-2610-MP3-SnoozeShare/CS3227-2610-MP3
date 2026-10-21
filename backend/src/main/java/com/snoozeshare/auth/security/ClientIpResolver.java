package com.snoozeshare.auth.security;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

/** Resolves the address used for abuse limits without trusting local client headers. */
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");
    private static final Pattern IPV6 = Pattern.compile("[0-9A-Fa-f:.]+");

    private final boolean trustForwardedHeaders;

    public ClientIpResolver(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public String resolve(HttpServletRequest request) {
        String directPeer = request.getRemoteAddr();
        if (!trustForwardedHeaders) {
            return directPeer;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return directPeer;
        }
        String candidate = forwardedFor.split(",", 2)[0].trim();
        return isIpAddress(candidate) ? candidate : directPeer;
    }

    private boolean isIpAddress(String value) {
        if (IPV4.matcher(value).matches()) {
            String[] octets = value.split("\\.");
            for (String octet : octets) {
                if (Integer.parseInt(octet) > 255) {
                    return false;
                }
            }
            return true;
        }
        return value.contains(":") && IPV6.matcher(value).matches();
    }
}
