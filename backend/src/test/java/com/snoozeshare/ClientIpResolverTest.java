package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.snoozeshare.auth.security.ClientIpResolver;

/** Verifies that proxy headers are opt-in, that only the proxy-set header is read, and that bad values fall back. */
class ClientIpResolverTest {

    @Test
    void localModeIgnoresProxyHeaders() {
        MockHttpServletRequest request = request("127.0.0.1", "203.0.113.10", "198.51.100.7");

        assertThat(new ClientIpResolver(false).resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void trustedProxyModeUsesTheProxySetClientAddress() {
        MockHttpServletRequest request = request("10.0.0.1", "203.0.113.10", null);

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void trustedProxyModeNeverReadsAClientForgeableForwardedFor() {
        MockHttpServletRequest request = request("10.0.0.1", null, "198.51.100.7, 10.0.0.2");

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void forwardedForCannotOverrideTheProxySetAddress() {
        MockHttpServletRequest request = request("10.0.0.1", "203.0.113.10", "198.51.100.7");

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void malformedProxyHeaderFallsBackToTheDirectPeer() {
        MockHttpServletRequest request = request("10.0.0.1", "not-an-ip", null);

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("10.0.0.1");
    }

    private MockHttpServletRequest request(String remoteAddress, String clientIp, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        if (clientIp != null) {
            request.addHeader("CF-Connecting-IP", clientIp);
        }
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }
}
