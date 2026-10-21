package com.snoozeshare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.domain.model.Message;

/** Spring Data JPA repository for ticket messages. */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("SELECT m FROM Message m "
            + "WHERE m.ticketId = :ticketId AND m.channel = :channel "
            + "ORDER BY m.sentAt ASC")
    List<Message> findThread(@Param("ticketId") UUID ticketId,
                             @Param("channel") ThreadChannel channel);

    @Query("SELECT m FROM Message m "
            + "WHERE m.ticketId = :ticketId AND m.channel = :channel "
            + "ORDER BY m.sentAt DESC LIMIT 1")
    Optional<Message> findLast(@Param("ticketId") UUID ticketId,
                               @Param("channel") ThreadChannel channel);
}
