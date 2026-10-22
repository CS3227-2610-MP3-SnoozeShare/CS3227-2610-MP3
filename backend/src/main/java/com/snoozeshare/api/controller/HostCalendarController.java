package com.snoozeshare.api.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
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

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.request.CreateHostBlockRequest;
import com.snoozeshare.api.response.AvailabilityBlockResponse;
import com.snoozeshare.api.response.HostCalendarResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.domain.model.AvailabilityBlock;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.service.AvailabilityService;
import com.snoozeshare.service.ListingService;

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
    private final ListingService listings;
    private final AvailabilityService availability;

    public HostCalendarController(HostIdentityResolver identityResolver,
                                  ListingService listings,
                                  AvailabilityService availability) {
        this.identityResolver = identityResolver;
        this.listings = listings;
        this.availability = availability;
    }

    @GetMapping("/listings/{listingId}/calendar")
    public HostCalendarResponse calendar(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID listingId,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam @Min(1) @Max(12) int month) {
        UUID hostId = identityResolver.resolve(identity);
        requireOwnedListing(listingId, hostId);
        YearMonth requestedMonth = YearMonth.of(year, month);
        LocalDate start = requestedMonth.atDay(1);
        LocalDate endExclusive = requestedMonth.plusMonths(1).atDay(1);
        List<AvailabilityBlock> monthBlocks = availability.blocksFor(listingId, start, endExclusive);
        List<HostCalendarResponse.Day> days = requestedMonth.atDay(1)
            .datesUntil(endExclusive)
            .map(date -> day(date, monthBlocks))
            .toList();
        List<AvailabilityBlockResponse> blockResponses = monthBlocks.stream()
            .map(this::toInclusiveResponse)
            .toList();
        return new HostCalendarResponse(year, month, days, blockResponses);
    }

    @PostMapping("/listings/{listingId}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityBlockResponse createBlock(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID listingId,
            @Valid @RequestBody CreateHostBlockRequest request) {
        UUID hostId = identityResolver.resolve(identity);
        requireOwnedListing(listingId, hostId);
        AvailabilityBlock created = availability.createHostBlock(
            listingId, request.startDate(), request.endDate(), hostId, request.reason());
        return toInclusiveResponse(created);
    }

    @DeleteMapping("/blocks/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBlock(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID blockId) {
        UUID hostId = identityResolver.resolve(identity);
        AvailabilityBlock block = availability.getBlock(blockId);
        requireOwnedListing(block.getPropertyId(), hostId);
        availability.removeHostBlock(blockId, hostId);
    }

    private HostCalendarResponse.Day day(LocalDate date, List<AvailabilityBlock> blocks) {
        boolean booked = blocks.stream().anyMatch(block -> "BOOKING".equals(block.getSource())
            && overlapsDay(block, date));
        boolean blocked = blocks.stream().anyMatch(block -> "HOST_BLOCK".equals(block.getSource())
            && overlapsDay(block, date));
        return new HostCalendarResponse.Day(date, booked, blocked);
    }

    private boolean overlapsDay(AvailabilityBlock block, LocalDate date) {
        return block.getStartDate().isBefore(date.plusDays(1)) && block.getEndDate().isAfter(date);
    }

    private AvailabilityBlockResponse toInclusiveResponse(AvailabilityBlock block) {
        return new AvailabilityBlockResponse(block.getId(), block.getPropertyId(),
            block.getStartDate(), block.getEndDate().minusDays(1), block.getSource(),
            block.getBookingId(), "HOST_BLOCK".equals(block.getSource()) ? block.getReason() : null);
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
