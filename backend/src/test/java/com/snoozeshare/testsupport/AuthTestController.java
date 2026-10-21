package com.snoozeshare.testsupport;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only endpoints used to prove URL and service authorization share a principal. */
@RestController
public class AuthTestController {

    @GetMapping(value = "/api/guest/ping", produces = MediaType.TEXT_PLAIN_VALUE)
    public String guest() {
        return "guest";
    }

    @GetMapping(value = "/api/host/ping", produces = MediaType.TEXT_PLAIN_VALUE)
    public String host() {
        return "host";
    }
}
