package com.snoozeshare.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.AvailabilityBlock;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.policy.StayDatePolicy;
import com.snoozeshare.repository.AvailabilityBlockRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.AvailabilityService;

@Service

public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Set<BookingStatus> CALENDAR_STATUSES = EnumSet.of(
            BookingStatus.PENDING, BookingStatus.CONFIRMED,
            BookingStatus.COMPLETED, BookingStatus.FORCE_COMPLETED);

    private final PropertyRepository properties;
    private final AvailabilityBlockRepository blocks;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final StayDatePolicy dates;

    public AvailabilityServiceImpl(PropertyRepository properties,
                                   AvailabilityBlockRepository blocks,
                                   BookingRepository bookings,
                                   UserRepository users,
                                   Clock clock) {
        this.properties = properties;
        this.blocks = blocks;
        this.bookings = bookings;
        this.users = users;
        this.dates = new StayDatePolicy(clock);
    }

    @Override
    public boolean isRangeAvailable(UUID propertyId, LocalDate start, LocalDate end) {
        return blocks.findOverlapping(propertyId, start, end).isEmpty()
                && bookings.findOverlapping(propertyId, start, end).isEmpty();
    }

    @Override
    public AvailabilityBlock createHostBlock(UUID propertyId, LocalDate start, LocalDate end,
                                              UUID hostId, String reason) {
        if (propertyId == null || start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("Property and a valid date range are required");
        }
        dates.requireNotInPast(start, "From date");
        User host = users.findById(hostId)
                .orElseThrow(() -> new IllegalArgumentException("Host does not exist"));
        if (host.getRole() != Role.HOST) {
            throw new IllegalArgumentException("Only hosts can create calendar blocks");
        }
        if (host.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Host account is not active");
        }
        LocalDate exclusiveEnd = end.plusDays(1);
        Property property = properties.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
        if (!property.getHostId().equals(hostId)) {
            throw new IllegalStateException("Host does not own this property");
        }
        if (!bookings.findOverlapping(propertyId, start, exclusiveEnd).isEmpty()
                || !blocks.findOverlapping(propertyId, start, exclusiveEnd).isEmpty()) {
            throw new IllegalStateException("Date range is not available");
        }
        String normalizedReason = reason == null ? null : reason.trim();
        if (normalizedReason != null && normalizedReason.isEmpty()) {
            normalizedReason = null;
        }
        return blocks.save(new AvailabilityBlock(UUID.randomUUID(), propertyId, start,
                exclusiveEnd, "HOST_BLOCK", null, normalizedReason));
    }

    @Override
    public void removeHostBlock(UUID blockId, UUID hostId) {
        AvailabilityBlock block = blocks.findById(blockId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Availability block does not exist"));
        if (!"HOST_BLOCK".equals(block.getSource())) {
            throw new IllegalStateException("Booking blocks cannot be removed");
        }
        Property property = properties.findById(block.getPropertyId())
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
        if (!property.getHostId().equals(hostId)) {
            throw new IllegalStateException("Host does not own this property");
        }
        blocks.deleteById(blockId);
    }

    @Override
    public List<AvailabilityBlock> blocksFor(UUID propertyId) {
        List<AvailabilityBlock> stored = blocks.findByPropertyId(propertyId);
        Set<UUID> covered = stored.stream().map(AvailabilityBlock::getBookingId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<AvailabilityBlock> shown = new ArrayList<>(stored);
        for (Booking booking : bookings.findByListingId(propertyId)) {
            if (CALENDAR_STATUSES.contains(booking.getStatus())
                    && !covered.contains(booking.getId())) {
                shown.add(new AvailabilityBlock(UUID.randomUUID(), propertyId,
                        booking.getStartDate(), booking.getEndDate(), "BOOKING",
                        booking.getId(), null));
            }
        }
        return shown;
    }
}
