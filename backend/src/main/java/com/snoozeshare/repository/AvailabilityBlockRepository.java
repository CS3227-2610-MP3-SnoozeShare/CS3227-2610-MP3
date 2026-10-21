package com.snoozeshare.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.model.AvailabilityBlock;

/** Spring Data JPA repository for property availability blocks. */
public interface AvailabilityBlockRepository
        extends JpaRepository<AvailabilityBlock, UUID> {

    List<AvailabilityBlock> findByPropertyId(UUID propertyId);

    @Query("SELECT ab FROM AvailabilityBlock ab "
            + "WHERE ab.propertyId = :propertyId "
            + "AND ab.startDate < :endDate AND ab.endDate > :startDate")
    List<AvailabilityBlock> findOverlapping(
            @Param("propertyId") UUID propertyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    void deleteByBookingId(UUID bookingId);
}
