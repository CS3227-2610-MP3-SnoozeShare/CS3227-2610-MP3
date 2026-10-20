package com.snoozeshare.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.snoozeshare.domain.enums.AmenityType;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A rental property listed on the platform. */
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @Column(name = "property_id")
    private UUID propertyId;

    @Column(name = "host_id", nullable = false)
    private UUID hostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false)
    private PropertyType propertyType;

    @Column(name = "street_address", nullable = false)
    private String streetAddress;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String region;

    @Column(name = "postal_code", nullable = false)
    private int postalCode;

    @Column(name = "max_guests", nullable = false)
    private int maxGuests;

    @Column(nullable = false)
    private int bedrooms;

    @Column(nullable = false)
    private int bathrooms;

    @Column(name = "base_nightly_rate", nullable = false,
            precision = 19, scale = 4)
    private BigDecimal baseNightlyRate;

    @Column(name = "check_in_time", nullable = false)
    private LocalTime checkInTime;

    @Column(name = "check_out_time", nullable = false)
    private LocalTime checkOutTime;

    @Convert(converter = AmenitySetConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private Set<AmenityType> amenities;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** JPA requires a no-arg constructor. */
    protected Property() {}

    /** Creates a new property. */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public Property(UUID propertyId, UUID hostId,
                    ListingStatus status, String title,
                    String description, PropertyType propertyType,
                    String streetAddress, String city, String region,
                    int postalCode, int maxGuests, int bedrooms,
                    int bathrooms, BigDecimal baseNightlyRate,
                    LocalTime checkInTime, LocalTime checkOutTime,
                    Set<AmenityType> amenities, Instant createdAt) {
        this.propertyId = propertyId;
        this.hostId = hostId;
        this.status = status;
        this.title = title;
        this.description = description;
        this.propertyType = propertyType;
        this.streetAddress = streetAddress;
        this.city = city;
        this.region = region;
        this.postalCode = postalCode;
        this.maxGuests = maxGuests;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.baseNightlyRate = baseNightlyRate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.amenities = Set.copyOf(amenities);
        this.createdAt = createdAt;
    }

    public UUID getPropertyId() {
        return propertyId;
    }

    public UUID getHostId() {
        return hostId;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(int postalCode) {
        this.postalCode = postalCode;
    }

    public int getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(int maxGuests) {
        this.maxGuests = maxGuests;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(int bedrooms) {
        this.bedrooms = bedrooms;
    }

    public int getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(int bathrooms) {
        this.bathrooms = bathrooms;
    }

    public BigDecimal getBaseNightlyRate() {
        return baseNightlyRate;
    }

    public void setBaseNightlyRate(BigDecimal baseNightlyRate) {
        this.baseNightlyRate = baseNightlyRate;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public Set<AmenityType> getAmenities() {
        return amenities;
    }

    public void setAmenities(Set<AmenityType> amenities) {
        this.amenities = Set.copyOf(amenities);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
