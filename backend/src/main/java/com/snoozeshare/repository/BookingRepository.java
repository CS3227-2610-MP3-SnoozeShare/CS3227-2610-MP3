package com.snoozeshare.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.model.Booking;

import jakarta.persistence.LockModeType;

/** Spring Data JPA repository for bookings. */
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("SELECT b FROM Booking b WHERE b.listingId = :propertyId "
            + "AND b.startDate < :endDate AND b.endDate > :startDate "
            + "AND b.status NOT IN ("
            + "com.snoozeshare.domain.enums.BookingStatus.REJECTED, "
            + "com.snoozeshare.domain.enums.BookingStatus.CANCELLED_BY_GUEST, "
            + "com.snoozeshare.domain.enums.BookingStatus.CANCELLED_BY_HOST, "
            + "com.snoozeshare.domain.enums.BookingStatus.FORCE_CANCELLED)")
    List<Booking> findOverlapping(@Param("propertyId") UUID propertyId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    List<Booking> findByGuestId(UUID guestId);

    List<Booking> findByListingId(UUID listingId);

    List<Booking> findByListingIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanOrderByStartDateAscIdAsc(
            UUID listingId, List<BookingStatus> statuses, LocalDate endDate, LocalDate startDate);

    @Query("SELECT b FROM Booking b WHERE b.listingId IN "
            + "(SELECT p.id FROM Property p WHERE p.hostId = :hostId) "
            + "AND b.status = com.snoozeshare.domain.enums.BookingStatus.PENDING")
    List<Booking> findByHostPending(@Param("hostId") UUID hostId);

    @Query("SELECT b FROM Booking b WHERE b.listingId IN "
            + "(SELECT p.id FROM Property p WHERE p.hostId = :hostId)")
    List<Booking> findByHost(@Param("hostId") UUID hostId);

    @Query("SELECT b FROM Booking b WHERE b.listingId IN "
            + "(SELECT p.id FROM Property p WHERE p.hostId = :hostId) "
            + "AND b.status = com.snoozeshare.domain.enums.BookingStatus.PENDING "
            + "ORDER BY b.createdAt DESC, b.id ASC")
    List<Booking> findPendingByHostOrderByCreatedAtDescIdAsc(@Param("hostId") UUID hostId);

    @Query("SELECT b FROM Booking b WHERE b.listingId IN "
            + "(SELECT p.id FROM Property p WHERE p.hostId = :hostId) "
            + "AND b.status <> com.snoozeshare.domain.enums.BookingStatus.PENDING "
            + "ORDER BY b.createdAt DESC, b.id ASC")
    List<Booking> findHistoryByHostOrderByCreatedAtDescIdAsc(@Param("hostId") UUID hostId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :bookingId")
    java.util.Optional<Booking> findByIdForUpdate(@Param("bookingId") UUID bookingId);

    @Query("SELECT b FROM Booking b WHERE b.listingId IN "
            + "(SELECT p.id FROM Property p WHERE p.hostId = :hostId) "
            + "AND (b.status = com.snoozeshare.domain.enums.BookingStatus.CONFIRMED "
            + "AND b.endDate >= :openCutoff OR EXISTS "
            + "(SELECT m.id FROM BookingMessage m WHERE m.bookingId = b.id)) "
            + "ORDER BY COALESCE((SELECT MAX(m.sentAt) FROM BookingMessage m "
            + "WHERE m.bookingId = b.id), b.createdAt) DESC, b.id ASC")
    List<Booking> findConversationBookingsByHost(@Param("hostId") UUID hostId,
                                                  @Param("openCutoff") LocalDate openCutoff,
                                                  Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.status = "
            + "com.snoozeshare.domain.enums.BookingStatus.CONFIRMED "
            + "AND b.endDate <= :cutoff")
    List<Booking> findConfirmedEndingOnOrBefore(
            @Param("cutoff") LocalDate cutoff);
}
