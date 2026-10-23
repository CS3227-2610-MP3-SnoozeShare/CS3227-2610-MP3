package com.snoozeshare.api.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.host.HostCalendarViews;
import com.snoozeshare.api.request.CreateHostBlockRequest;
import com.snoozeshare.api.response.AvailabilityBlockResponse;
import com.snoozeshare.api.response.HostCalendarResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Authenticated Host calendar reads and availability-block mutations. */
@RestController
@Validated
@RequestMapping("/api/host")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostCalendarController {

    private final HostIdentityResolver identityResolver;
    private final HostCalendarViews calendars;

    public HostCalendarController(HostIdentityResolver identityResolver, HostCalendarViews calendars) {
        this.identityResolver = identityResolver;
        this.calendars = calendars;
    }

    @GetMapping("/listings/{listingId}/calendar")
    public HostCalendarResponse calendar(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID listingId,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        return calendars.calendar(listingId, identityResolver.resolve(identity), year, month);
    }

    @PostMapping("/listings/{listingId}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityBlockResponse createBlock(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID listingId,
            @Valid @RequestBody CreateHostBlockRequest request) {
        return calendars.createBlock(listingId, identityResolver.resolve(identity), request);
    }

    @DeleteMapping("/blocks/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBlock(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID blockId) {
        calendars.removeBlock(blockId, identityResolver.resolve(identity));
    }
}
