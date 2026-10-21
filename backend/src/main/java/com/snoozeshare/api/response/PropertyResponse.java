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

public record PropertyResponse(
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
        Instant createdAt) {

    public static PropertyResponse from(Property p) {
        return new PropertyResponse(p.getId(), p.getHostId(), p.getStatus(),
                p.getTitle(), p.getDescription(), p.getPropertyType(),
                p.getStreetAddress(), p.getCity(), p.getRegion(), p.getPostalCode(),
                p.getMaxGuests(), p.getBedrooms(), p.getBathrooms(),
                p.getBaseNightlyRate(), p.getCheckInTime(), p.getCheckOutTime(),
                p.getAmenities(), p.getCreatedAt());
    }
}
