package com.snoozeshare.api.host;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.snoozeshare.api.request.CreateHostBlockRequest;
import com.snoozeshare.api.response.AvailabilityBlockResponse;
import com.snoozeshare.api.response.HostCalendarResponse;
import com.snoozeshare.domain.model.AvailabilityBlock;
import com.snoozeshare.service.AvailabilityService;

/** Host calendar months and availability blocks as response DTOs; ownership is checked first. */
@Component
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostCalendarViews {

    private final HostListingViews listings;
    private final AvailabilityService availability;

    public HostCalendarViews(HostListingViews listings, AvailabilityService availability) {
        this.listings = listings;
        this.availability = availability;
    }

    public HostCalendarResponse calendar(UUID listingId, UUID hostId, int year, int month) {
        listings.requireOwned(listingId, hostId);
        YearMonth requestedMonth = YearMonth.of(year, month);
        LocalDate start = requestedMonth.atDay(1);
        LocalDate endExclusive = requestedMonth.plusMonths(1).atDay(1);
        List<AvailabilityBlock> monthBlocks = availability.blocksFor(listingId, start, endExclusive);
        List<HostCalendarResponse.Day> days = start.datesUntil(endExclusive)
            .map(date -> day(date, monthBlocks))
            .toList();
        List<AvailabilityBlockResponse> blockResponses = monthBlocks.stream()
            .map(HostCalendarViews::toInclusiveResponse)
            .toList();
        return new HostCalendarResponse(year, month, days, blockResponses);
    }

    public AvailabilityBlockResponse createBlock(UUID listingId, UUID hostId, CreateHostBlockRequest request) {
        listings.requireOwned(listingId, hostId);
        AvailabilityBlock created = availability.createHostBlock(
            listingId, request.startDate(), request.endDate(), hostId, request.reason());
        return toInclusiveResponse(created);
    }

    public void removeBlock(UUID blockId, UUID hostId) {
        AvailabilityBlock block = availability.getBlock(blockId);
        listings.requireOwned(block.getPropertyId(), hostId);
        availability.removeHostBlock(blockId, hostId);
    }

    private static HostCalendarResponse.Day day(LocalDate date, List<AvailabilityBlock> blocks) {
        boolean booked = blocks.stream().anyMatch(block -> "BOOKING".equals(block.getSource())
            && overlapsDay(block, date));
        boolean blocked = blocks.stream().anyMatch(block -> "HOST_BLOCK".equals(block.getSource())
            && overlapsDay(block, date));
        return new HostCalendarResponse.Day(date, booked, blocked);
    }

    private static boolean overlapsDay(AvailabilityBlock block, LocalDate date) {
        return block.getStartDate().isBefore(date.plusDays(1)) && block.getEndDate().isAfter(date);
    }

    private static AvailabilityBlockResponse toInclusiveResponse(AvailabilityBlock block) {
        return new AvailabilityBlockResponse(block.getId(), block.getPropertyId(),
            block.getStartDate(), block.getEndDate().minusDays(1), block.getSource(),
            block.getBookingId(), "HOST_BLOCK".equals(block.getSource()) ? block.getReason() : null);
    }
}
