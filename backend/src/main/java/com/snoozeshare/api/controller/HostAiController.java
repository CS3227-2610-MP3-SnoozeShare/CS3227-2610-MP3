package com.snoozeshare.api.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.ai.host.HostListingDescriptionService;
import com.snoozeshare.api.request.HostAiListingDescriptionRequest;
import com.snoozeshare.api.response.HostAiListingDescriptionResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.config.ConditionalOnDatabase;

import jakarta.validation.Valid;

/** Host-only AI operations. */
@RestController
@Validated
@RequestMapping("/api/host/ai")
@ConditionalOnDatabase
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostAiController {

    private final HostListingDescriptionService descriptions;

    public HostAiController(HostListingDescriptionService descriptions) {
        this.descriptions = descriptions;
    }

    @PostMapping("/listing-description")
    public ResponseEntity<HostAiListingDescriptionResponse> listingDescription(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @Valid @RequestBody HostAiListingDescriptionRequest request) {
        String requestId = UUID.randomUUID().toString();
        return ResponseEntity.ok(descriptions.run(identity, request, requestId));
    }
}
