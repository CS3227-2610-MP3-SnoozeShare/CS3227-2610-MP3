package com.snoozeshare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.enums.TicketStatus;
import com.snoozeshare.domain.model.Ticket;

import jakarta.persistence.LockModeType;

/** Spring Data JPA repository for dispute tickets. */
public interface TicketRepository
        extends JpaRepository<Ticket, UUID>, TicketQueueRepository {

    List<Ticket> findByStatus(TicketStatus status);

    long countByStatusAndAssignedAgentIdIsNull(TicketStatus status);

    /** Locks the ticket row; the caller takes this as its first read of the ticket (SD10). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> findByIdForUpdate(@Param("id") UUID id);

    List<Ticket> findByBookingId(UUID bookingId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END "
            + "FROM Ticket t WHERE LOWER(t.category) = LOWER(:label)")
    boolean existsByCategory(@Param("label") String categoryLabel);

    List<Ticket> findByRaisedByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT t FROM Ticket t WHERE t.bookingId IN "
            + "(SELECT b.id FROM Booking b WHERE b.guestId = :userId) "
            + "ORDER BY t.createdAt DESC")
    List<Ticket> findByGuest(@Param("userId") UUID userId);

    @Query("SELECT t FROM Ticket t WHERE t.bookingId IN "
            + "(SELECT b.id FROM Booking b WHERE b.listingId IN "
            + "(SELECT p.id FROM Property p WHERE p.hostId = :userId)) "
            + "ORDER BY t.createdAt DESC")
    List<Ticket> findByHost(@Param("userId") UUID userId);
}
