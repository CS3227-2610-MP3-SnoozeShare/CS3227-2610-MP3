package com.snoozeshare.api.request;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.snoozeshare.domain.enums.AmenityType;
import com.snoozeshare.domain.enums.PropertyType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Update an existing property listing (does not change status). */
@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateListingRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull PropertyType propertyType,
        @NotBlank String streetAddress,
        @NotBlank String city,
        @NotBlank String region,
        @Positive int postalCode,
        @Positive int maxGuests,
        @PositiveOrZero int bedrooms,
        @PositiveOrZero int bathrooms,
        @NotNull @Positive BigDecimal baseNightlyRate,
        @NotNull LocalTime checkInTime,
        @NotNull LocalTime checkOutTime,
        Set<AmenityType> amenities) {
}
