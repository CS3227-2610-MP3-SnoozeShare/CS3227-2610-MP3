package com.snoozeshare.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.host.HostListingViews;
import com.snoozeshare.api.request.CreateListingRequest;
import com.snoozeshare.api.request.UpdateListingRequest;
import com.snoozeshare.api.request.UpdateListingStatusRequest;
import com.snoozeshare.api.response.HostListingDetailResponse;
import com.snoozeshare.api.response.HostListingSummaryResponse;
import com.snoozeshare.api.response.PropertyResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;

import jakarta.validation.Valid;

/** Authenticated Host listing reads and mutations. */
@RestController
@Validated
@RequestMapping("/api/host/listings")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostListingController {

    private final HostIdentityResolver identityResolver;
    private final HostListingViews views;

    public HostListingController(HostIdentityResolver identityResolver, HostListingViews views) {
        this.identityResolver = identityResolver;
        this.views = views;
    }

    @GetMapping
    public List<HostListingSummaryResponse> list(
        @AuthenticationPrincipal FirebaseAuthentication identity) {
        return views.summaries(identityResolver.resolve(identity));
    }

    @GetMapping("/{listingId}")
    public HostListingDetailResponse detail(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @PathVariable UUID listingId) {
        return views.detail(listingId, identityResolver.resolve(identity));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @Valid @RequestBody CreateListingRequest request) {
        return views.create(request, identityResolver.resolve(identity));
    }

    @PutMapping("/{listingId}")
    public PropertyResponse update(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @PathVariable UUID listingId,
        @Valid @RequestBody UpdateListingRequest request) {
        return views.update(listingId, request, identityResolver.resolve(identity));
    }

    @PutMapping("/{listingId}/status")
    public PropertyResponse updateStatus(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @PathVariable UUID listingId,
        @Valid @RequestBody UpdateListingStatusRequest request) {
        return views.updateStatus(listingId, request.status(), identityResolver.resolve(identity));
    }
}
