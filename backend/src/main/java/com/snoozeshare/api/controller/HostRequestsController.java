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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.request.BookingDecisionRequest;
import com.snoozeshare.api.response.BookingResponse;
import com.snoozeshare.api.response.HostBookingRowResponse;
import com.snoozeshare.api.response.PageResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.service.BookingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Authenticated, host-scoped booking request reads and decisions. */
@RestController
@Validated
@RequestMapping("/api/host/requests")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostRequestsController {

    private final HostIdentityResolver identityResolver;
    private final BookingService bookings;

    public HostRequestsController(HostIdentityResolver identityResolver, BookingService bookings) {
        this.identityResolver = identityResolver;
        this.bookings = bookings;
    }

    @GetMapping
    public PageResponse<HostBookingRowResponse> list(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @RequestParam String view,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        UUID hostId = identityResolver.resolve(identity);
        List<HostBookingRowResponse> rows;
        if ("pending".equals(view)) {
            rows = bookings.pendingRequestRowsFor(hostId, page, size).stream()
                    .map(HostBookingRowResponse::from).toList();
        } else if ("history".equals(view)) {
            rows = bookings.historyRowsFor(hostId, page, size).stream()
                    .map(HostBookingRowResponse::from).toList();
        } else {
            throw new HostApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
        return PageResponse.from(rows, page, size);
    }

    @PostMapping("/{bookingId}/decision")
    public BookingResponse decide(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID bookingId,
            @Valid @RequestBody BookingDecisionRequest request) {
        UUID hostId = identityResolver.resolve(identity);
        Booking booking;
        try {
            booking = bookings.decide(bookingId, request.approve(), hostId, request.message());
        } catch (IllegalArgumentException exception) {
            throw new HostApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
        }
        return BookingResponse.from(booking);
    }
}
