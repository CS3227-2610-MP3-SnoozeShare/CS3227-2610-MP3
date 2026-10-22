package com.snoozeshare.service.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.policy.StayDatePolicy;
import com.snoozeshare.domain.validation.DomainValidation;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.SearchCriteria;
import com.snoozeshare.service.AuditRecord;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.AvailabilityService;
import com.snoozeshare.service.ListingService;
import com.snoozeshare.service.PriceBreakdown;
import com.snoozeshare.service.SearchResult;
import com.snoozeshare.service.UserService;

@Service
@ConditionalOnDatabase
public class ListingServiceImpl implements ListingService {

    private final StayDatePolicy searchDates;
    private final PropertyRepository properties;
    private final AvailabilityService availability;
    private final UserService users;
    private final AuditService audit;

    public ListingServiceImpl(Clock clock,
                              PropertyRepository properties,
                              AvailabilityService availability,
                              UserService users,
                              AuditService audit) {
        this.searchDates = new StayDatePolicy(clock);
        this.properties = properties;
        this.availability = availability;
        this.users = users;
        this.audit = audit;
    }

    @Override
    public List<SearchResult> search(SearchCriteria criteria) {
        searchDates.requireValidSearchRange(criteria.startDate(), criteria.endDate());
        List<Property> candidates = properties.findBySearchCriteria(criteria);
        boolean hasDateRange = criteria.startDate() != null && criteria.endDate() != null;
        if (!hasDateRange) {
            return candidates.stream()
                    .map(p -> new SearchResult(p, true))
                    .toList();
        }
        List<SearchResult> available = new ArrayList<>();
        List<SearchResult> unavailable = new ArrayList<>();
        for (Property property : candidates) {
            if (availability.isRangeAvailable(property.getId(),
                    criteria.startDate(), criteria.endDate())) {
                available.add(new SearchResult(property, true));
            } else {
                unavailable.add(new SearchResult(property, false));
            }
        }
        List<SearchResult> results = new ArrayList<>(available);
        results.addAll(unavailable);
        return results;
    }

    @Override
    public Property getDetail(UUID propertyId) {
        return properties.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
    }

    @Override
    public PriceBreakdown estimateCost(UUID propertyId, LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates must not be null");
        }
        long nights = ChronoUnit.DAYS.between(start, end);
        if (nights <= 0) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        Property property = getDetail(propertyId);
        BigDecimal totalAmount = property.getBaseNightlyRate()
                .multiply(BigDecimal.valueOf(nights));
        return new PriceBreakdown(property.getBaseNightlyRate(), (int) nights, totalAmount);
    }

    @Override
    public List<Property> findByHostId(UUID hostId) {
        return properties.findByHostId(hostId);
    }

    @Override
    @Transactional
    public Property create(Property draft, UUID hostId) {
        User host = requireActiveHost(hostId);
        validateDraft(draft);
        if (draft.getId() != null && properties.findById(draft.getId()).isPresent()) {
            throw new IllegalArgumentException("Property ID already exists");
        }
        Property saved = new Property(
                draft.getId() == null ? UUID.randomUUID() : draft.getId(),
                host.getId(), draft.getStatus(), draft.getTitle(), draft.getDescription(),
                draft.getPropertyType(), draft.getStreetAddress(), draft.getCity(),
                draft.getRegion(), draft.getPostalCode(), draft.getMaxGuests(),
                draft.getBedrooms(), draft.getBathrooms(), draft.getBaseNightlyRate(),
                draft.getCheckInTime(), draft.getCheckOutTime(), draft.getAmenities(),
                draft.getCreatedAt() == null ? java.time.Instant.now() : draft.getCreatedAt());
        Property persisted = properties.save(saved);
        audit.record(AuditRecord.builder(host.getId(), AuditAction.LISTING_CREATED,
                        "Property", persisted.getId())
                .status(null, persisted.getStatus()).subject(host.getId())
                .reason("Listing created: " + persisted.getTitle()).build());
        return persisted;
    }

    @Override
    @Transactional
    public Property update(Property draft, UUID hostId) {
        User host = requireActiveHost(hostId);
        if (draft == null || draft.getId() == null) {
            throw new IllegalArgumentException("Property ID must not be null");
        }
        Property existing = properties.findById(draft.getId())
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
        if (!host.getId().equals(existing.getHostId())) {
            throw new IllegalStateException("Host does not own this property");
        }
        validateDraft(draft);
        existing.setTitle(draft.getTitle());
        existing.setDescription(draft.getDescription());
        existing.setPropertyType(draft.getPropertyType());
        existing.setStreetAddress(draft.getStreetAddress());
        existing.setCity(draft.getCity());
        existing.setRegion(draft.getRegion());
        existing.setPostalCode(draft.getPostalCode());
        existing.setMaxGuests(draft.getMaxGuests());
        existing.setBedrooms(draft.getBedrooms());
        existing.setBathrooms(draft.getBathrooms());
        existing.setBaseNightlyRate(draft.getBaseNightlyRate());
        existing.setCheckInTime(draft.getCheckInTime());
        existing.setCheckOutTime(draft.getCheckOutTime());
        existing.setAmenities(draft.getAmenities());
        Property persisted = properties.save(existing);
        audit.record(AuditRecord.builder(host.getId(), AuditAction.LISTING_UPDATED,
                        "Property", persisted.getId())
                .subject(host.getId()).reason("Listing details updated").build());
        return persisted;
    }

    @Override
    @Transactional
    public Property updateStatus(UUID propertyId, ListingStatus status, UUID hostId) {
        User host = requireActiveHost(hostId);
        if (status == null) {
            throw new IllegalArgumentException("Listing status must not be null");
        }
        Property existing = properties.findById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
        if (!host.getId().equals(existing.getHostId())) {
            throw new IllegalStateException("Host does not own this property");
        }
        if (existing.getStatus() == status) {
            return existing;
        }
        ListingStatus before = existing.getStatus();
        existing.setStatus(status);
        Property persisted = properties.save(existing);
        audit.record(AuditRecord.builder(host.getId(), AuditAction.LISTING_STATUS_CHANGED,
                        "Property", propertyId)
                .status(before, persisted.getStatus()).subject(host.getId()).build());
        return persisted;
    }

    private User requireActiveHost(UUID hostId) {
        if (hostId == null) {
            throw new IllegalStateException("Host ID must not be null");
        }
        User host = users.findById(hostId);
        AuthorizationService.requireRole(Role.HOST, host);
        if (host.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
        return host;
    }

    private static void validateDraft(Property draft) {
        if (draft == null) {
            throw new IllegalArgumentException("Property must not be null");
        }
        DomainValidation.requireText(draft.getTitle(), "Title");
        DomainValidation.requireText(draft.getDescription(), "Description");
        if (draft.getPropertyType() == null) {
            throw new IllegalArgumentException("Property Type must be selected");
        }
        if (draft.getStatus() == null) {
            throw new IllegalArgumentException("Status must be selected");
        }
        if (draft.getMaxGuests() <= 0) {
            throw new IllegalArgumentException("Max Guests must be positive");
        }
        if (draft.getBedrooms() < 0) {
            throw new IllegalArgumentException("Bedrooms must be non-negative");
        }
        if (draft.getBathrooms() < 0) {
            throw new IllegalArgumentException("Bathrooms must be non-negative");
        }
        DomainValidation.requirePositive(draft.getBaseNightlyRate(), "Rate per night");
        if (draft.getBaseNightlyRate().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Rate per night must have at most 2 decimal places");
        }
        if (draft.getCheckInTime() == null) {
            throw new IllegalArgumentException("Check-in Time must be provided");
        }
        if (draft.getCheckOutTime() == null) {
            throw new IllegalArgumentException("Check-out Time must be provided");
        }
        DomainValidation.requireText(draft.getStreetAddress(), "Street Address");
        DomainValidation.requireText(draft.getCity(), "City");
        DomainValidation.requireText(draft.getRegion(), "Region");
        if (draft.getPostalCode() <= 0) {
            throw new IllegalArgumentException("Postal Code must be a positive whole number");
        }
    }
}
