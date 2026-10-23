package com.snoozeshare.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.model.TicketAssistant;

import jakarta.persistence.LockModeType;

/** Caller-owned transactions provide locking and compare-and-set turn ownership. */
public interface TicketAssistantRepository extends JpaRepository<TicketAssistant, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from TicketAssistant a where a.ticketId = :id")
    Optional<TicketAssistant> findByTicketIdForUpdate(@Param("id") UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "insert into ticket_assistants (ticket_id, state, rounds, flagged, created_at, updated_at) "
            + "values (:id, 'PENDING', 0, false, :now, :now) on conflict (ticket_id) do nothing", nativeQuery = true)
    int insertPending(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update ticket_assistants a set state = 'PENDING', updated_at = :now "
            + "where a.ticket_id = :id and a.state = 'WAITING_GUEST' "
            + "and exists (select 1 from tickets t where t.id = a.ticket_id "
            + "and t.status in ('OPEN', 'IN_REVIEW'))", nativeQuery = true)
    int claimWaiting(@Param("id") UUID id, @Param("now") Instant now);
}
