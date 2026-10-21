package com.snoozeshare;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.snoozeshare.auth.security.ClientIpResolver;

/** Verifies that proxy headers are opt-in and fall back safely. */
class ClientIpResolverTest {

    @Test
    void localModeIgnoresForwardedHeaders() {
        MockHttpServletRequest request = request("127.0.0.1", "203.0.113.10");

        assertThat(new ClientIpResolver(false).resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void trustedProxyModeUsesTheFirstForwardedAddress() {
        MockHttpServletRequest request = request("10.0.0.1", "203.0.113.10, 10.0.0.2");

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void malformedForwardedHeaderFallsBackToTheDirectPeer() {
        MockHttpServletRequest request = request("10.0.0.1", "not-an-ip");

        assertThat(new ClientIpResolver(true).resolve(request)).isEqualTo("10.0.0.1");
    }

    private MockHttpServletRequest request(String remoteAddress, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
