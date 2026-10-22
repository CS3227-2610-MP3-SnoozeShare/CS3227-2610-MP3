package com.snoozeshare.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.snoozeshare.domain.enums.AmenityType;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.service.ListingSummaryMetrics;

/** Property fields and aggregated metrics shown on a Host listing card. */
public record HostListingSummaryResponse(
        UUID id,
        UUID hostId,
        ListingStatus status,
        String title,
        String description,
        PropertyType propertyType,
        String streetAddress,
        String city,
        String region,
        int postalCode,
        int maxGuests,
        int bedrooms,
        int bathrooms,
        BigDecimal baseNightlyRate,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        Set<AmenityType> amenities,
        Instant createdAt,
        long bookingCount,
        double averageRating) {

    public static HostListingSummaryResponse from(Property property, ListingSummaryMetrics metrics) {
        return new HostListingSummaryResponse(
                property.getId(), property.getHostId(), property.getStatus(),
                property.getTitle(), property.getDescription(), property.getPropertyType(),
                property.getStreetAddress(), property.getCity(), property.getRegion(),
                property.getPostalCode(), property.getMaxGuests(), property.getBedrooms(),
                property.getBathrooms(), property.getBaseNightlyRate(), property.getCheckInTime(),
                property.getCheckOutTime(), property.getAmenities(), property.getCreatedAt(),
                metrics.bookingCount(), metrics.averageRating());
    }
}
