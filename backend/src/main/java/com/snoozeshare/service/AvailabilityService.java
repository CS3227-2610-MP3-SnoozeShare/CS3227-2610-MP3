package com.snoozeshare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.AvailabilityBlock;

/**
 * Decides whether a property's dates are free and manages the host's own date blocks.
 */
public interface AvailabilityService {

    boolean isRangeAvailable(UUID propertyId, LocalDate start, LocalDate end);

    AvailabilityBlock createHostBlock(UUID propertyId, LocalDate start, LocalDate end,
                                      UUID hostId, String reason);

    void removeHostBlock(UUID blockId, UUID hostId);

    List<AvailabilityBlock> blocksFor(UUID propertyId);
}
