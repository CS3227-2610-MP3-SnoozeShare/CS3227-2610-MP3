package com.snoozeshare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.model.Property;

/**
 * Listing search, detail and host-side listing management.
 *
 * Mutating methods require an active user with the Host role and, for existing listings,
 * ownership. They run in one transaction together with their audit entry.
 */
public interface ListingService {
    /**
     * Finds listings matching the criteria. When a date range is given, listings that are free for it
     * come first and the rest follow flagged as unavailable.
     *
     * @param criteria filters; the date range, if present, must be valid for searching
     * @return matching listings with their availability flag
     */
    List<SearchResult> search(SearchCriteria criteria);

    /**
     * @param propertyId the listing id
     * @return the listing
     * @throws IllegalArgumentException if it does not exist
     */
    Property getDetail(UUID propertyId);

    /**
     * Prices a stay at the listing's current nightly rate.
     *
     * @param start check-in day
     * @param end check-out day; must be after {@code start}
     * @throws IllegalArgumentException if a date is null, the range is empty, or the listing does not
     *         exist
     */
    PriceBreakdown estimateCost(UUID propertyId, LocalDate start, LocalDate end);

    /**
     * @param hostId the host
     * @return that host's listings, whatever their status
     */
    List<Property> findByHostId(UUID hostId);

    /**
     * Creates a listing owned by {@code hostId}. The draft's status is honoured, so a host can save a
     * listing as inactive.
     *
     * @param draft the listing details; its id and creation time are filled in when null
     * @param hostId the acting host
     * @return the saved listing
     * @throws IllegalArgumentException if a field fails validation or the id already exists
     * @throws IllegalStateException if the user is not an active host
     */
    Property create(Property draft, UUID hostId);

    /**
     * Edits the descriptive fields of a listing. Status and ownership are kept; use
     * {@link #updateStatus} to change the status.
     *
     * @throws IllegalArgumentException if the draft has no id, fails validation, or the listing does
     *         not exist
     * @throws IllegalStateException if the host does not own the listing
     */
    Property update(Property draft, UUID hostId);

    /**
     * Sets a listing active or inactive. A no-op when the status is unchanged.
     *
     * @throws IllegalArgumentException if the status is null or the listing does not exist
     * @throws IllegalStateException if the host does not own the listing
     */
    Property updateStatus(UUID propertyId, ListingStatus status, UUID hostId);
}
