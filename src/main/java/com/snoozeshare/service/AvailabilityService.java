package com.snoozeshare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.AvailabilityBlock;

/**
 * Decides whether a property's dates are free and manages the host's own date blocks.
 *
 * Dates are held by two things: stored availability blocks (host blocks and booking blocks) and
 * bookings that are pending or confirmed. A range is available only if neither overlaps it.
 */
public interface AvailabilityService {
    /**
     * Whether the half-open range [start, end) overlaps no block and no pending or confirmed booking.
     *
     * @param propertyId the listing to check
     * @param start first night (check-in day)
     * @param end check-out day, exclusive
     * @return true if every night in the range is free
     */
    boolean isRangeAvailable(UUID propertyId, LocalDate start, LocalDate end);

    /**
     * Blocks dates on a listing so guests cannot book them.
     *
     * @param propertyId the listing to block
     * @param start first blocked day
     * @param end last blocked day, <b>inclusive</b> (stored internally as an exclusive end)
     * @param hostId the acting host, who must own the listing
     * @param reason optional note; blank is stored as none
     * @return the saved block (source {@code HOST_BLOCK})
     * @throws IllegalArgumentException if the range is invalid or the listing does not exist
     * @throws IllegalStateException if the host does not own the listing or the dates overlap a block
     *         or booking
     */
    AvailabilityBlock createHostBlock(UUID propertyId, LocalDate start, LocalDate end,
                                      UUID hostId, String reason);

    /**
     * Removes a block the host created. Booking blocks are owned by the booking lifecycle and cannot
     * be removed here.
     *
     * @param blockId the block to remove
     * @param hostId the acting host, who must own the listing
     * @throws IllegalArgumentException if the block or its listing does not exist
     * @throws IllegalStateException if the block is a booking block or the host does not own the
     *         listing
     */
    void removeHostBlock(UUID blockId, UUID hostId);

    /**
     * Everything the host calendar should draw for a listing: the stored blocks plus a synthetic
     * booking block for any pending, confirmed or completed booking whose stored block is missing.
     *
     * @param propertyId the listing to show
     * @return stored and synthetic blocks, in no guaranteed order
     */
    List<AvailabilityBlock> blocksFor(UUID propertyId);
}
