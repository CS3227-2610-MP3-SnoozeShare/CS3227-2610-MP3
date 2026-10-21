package com.snoozeshare.api.mapper;

import java.util.Set;

import com.snoozeshare.api.request.CreateListingRequest;
import com.snoozeshare.api.request.UpdateListingRequest;
import com.snoozeshare.domain.model.Property;

/** Maps listing request DTOs to domain entities. */
public final class ListingMapper {

    private ListingMapper() {
    }

    public static Property toDraft(CreateListingRequest req) {
        return new Property(null, null, req.status(), req.title(), req.description(),
                req.propertyType(), req.streetAddress(), req.city(), req.region(),
                req.postalCode(), req.maxGuests(), req.bedrooms(), req.bathrooms(),
                req.baseNightlyRate(), req.checkInTime(), req.checkOutTime(),
                req.amenities() != null ? req.amenities() : Set.of(), null);
    }

    public static Property toDraft(java.util.UUID propertyId, UpdateListingRequest req) {
        return new Property(propertyId, null, null, req.title(), req.description(),
                req.propertyType(), req.streetAddress(), req.city(), req.region(),
                req.postalCode(), req.maxGuests(), req.bedrooms(), req.bathrooms(),
                req.baseNightlyRate(), req.checkInTime(), req.checkOutTime(),
                req.amenities() != null ? req.amenities() : Set.of(), null);
    }
}
