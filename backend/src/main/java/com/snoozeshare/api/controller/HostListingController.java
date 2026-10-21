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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.mapper.ListingMapper;
import com.snoozeshare.api.request.CreateListingRequest;
import com.snoozeshare.api.request.UpdateListingRequest;
import com.snoozeshare.api.request.UpdateListingStatusRequest;
import com.snoozeshare.api.response.HostListingDetailResponse;
import com.snoozeshare.api.response.ListingMetricsResponse;
import com.snoozeshare.api.response.PageResponse;
import com.snoozeshare.api.response.PropertyResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.service.ListingMetricsService;
import com.snoozeshare.service.ListingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Authenticated Host listing reads and mutations. */
@RestController
@Validated
@RequestMapping("/api/host/listings")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostListingController {

    private final HostIdentityResolver identityResolver;
    private final ListingService listings;
    private final ListingMetricsService metrics;

    public HostListingController(
        HostIdentityResolver identityResolver,
        ListingService listings,
        ListingMetricsService metrics) {
        this.identityResolver = identityResolver;
        this.listings = listings;
        this.metrics = metrics;
    }

    @GetMapping
    public PageResponse<PropertyResponse> list(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        UUID hostId = identityResolver.resolve(identity);
        List<PropertyResponse> fetched = listings.findByHostId(hostId, page, size).stream()
            .map(PropertyResponse::from)
            .toList();
        return PageResponse.from(fetched, page, size);
    }

    @GetMapping("/{listingId}")
    public HostListingDetailResponse detail(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @PathVariable UUID listingId) {
        UUID hostId = identityResolver.resolve(identity);
        Property property = requireOwnedListing(listingId, hostId);
        return new HostListingDetailResponse(
            PropertyResponse.from(property), ListingMetricsResponse.from(metrics.metricsFor(property.getId())));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @Valid @RequestBody CreateListingRequest request) {
        UUID hostId = identityResolver.resolve(identity);
        return PropertyResponse.from(listings.create(ListingMapper.toDraft(request), hostId));
    }

    @PutMapping("/{listingId}")
    public PropertyResponse update(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @PathVariable UUID listingId,
        @Valid @RequestBody UpdateListingRequest request) {
        UUID hostId = identityResolver.resolve(identity);
        requireOwnedListing(listingId, hostId);
        return PropertyResponse.from(listings.update(ListingMapper.toDraft(listingId, request), hostId));
    }

    @PutMapping("/{listingId}/status")
    public PropertyResponse updateStatus(
        @AuthenticationPrincipal FirebaseAuthentication identity,
        @PathVariable UUID listingId,
        @Valid @RequestBody UpdateListingStatusRequest request) {
        UUID hostId = identityResolver.resolve(identity);
        requireOwnedListing(listingId, hostId);
        return PropertyResponse.from(listings.updateStatus(listingId, request.status(), hostId));
    }

    private Property requireOwnedListing(UUID listingId, UUID hostId) {
        Property property;
        try {
            property = listings.getDetail(listingId);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
        if (!hostId.equals(property.getHostId())) {
            throw notFound();
        }
        return property;
    }

    private HostApiException notFound() {
        return new HostApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
}
