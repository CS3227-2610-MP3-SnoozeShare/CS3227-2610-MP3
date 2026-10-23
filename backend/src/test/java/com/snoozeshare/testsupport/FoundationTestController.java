package com.snoozeshare.testsupport;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.CurrentUserId;
import com.snoozeshare.api.request.SaveNotesRequest;
import com.snoozeshare.service.ThrowFixture;

/** Test-only route for exercising the authenticated agent namespace. */
@RestController
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class FoundationTestController {

    private final AtomicInteger noteCalls = new AtomicInteger();

    @GetMapping(value = "/api/agent/_test/ping", produces = MediaType.TEXT_PLAIN_VALUE)
    public String ping() {
        return "agent";
    }

    @GetMapping(value = "/api/agent/_test/actor", produces = MediaType.TEXT_PLAIN_VALUE)
    public String actor(@CurrentUserId UUID agentId) {
        return agentId.toString();
    }

    @PostMapping(value = "/api/agent/_test/notes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String notes(@RequestBody SaveNotesRequest request) {
        noteCalls.incrementAndGet();
        return request.notes();
    }

    public int noteCallCount() {
        return noteCalls.get();
    }

    @GetMapping("/api/agent/_test/uuid/{id}")
    public UUID uuid(@PathVariable UUID id) {
        return id;
    }

    @GetMapping("/api/agent/_test/repository-error")
    public void repositoryError() {
        ThrowFixture.repositoryFailure();
    }

    @GetMapping("/api/agent/_test/service-invalid")
    public void serviceInvalid() {
        ThrowFixture.invalidRequest();
    }

    @GetMapping("/api/agent/_test/plain-invalid")
    public void plainInvalid() {
        com.snoozeshare.testsupport.PlainThrowFixture.invalidRequest();
    }

    @GetMapping("/api/agent/_test/plain-secret")
    public void plainSecret() {
        com.snoozeshare.testsupport.PlainThrowFixture.secretFailure();
    }
}
