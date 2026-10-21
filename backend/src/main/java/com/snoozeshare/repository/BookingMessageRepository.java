package com.snoozeshare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.model.BookingMessage;

/** Spring Data JPA repository for booking messages. */
public interface BookingMessageRepository
        extends JpaRepository<BookingMessage, UUID> {

    @Query("SELECT m FROM BookingMessage m "
            + "WHERE m.bookingId = :bookingId "
            + "ORDER BY m.sentAt ASC")
    List<BookingMessage> findThread(@Param("bookingId") UUID bookingId);

    @Query("SELECT m FROM BookingMessage m "
            + "WHERE m.bookingId = :bookingId "
            + "ORDER BY m.sentAt DESC LIMIT 1")
    Optional<BookingMessage> findLast(@Param("bookingId") UUID bookingId);
}
