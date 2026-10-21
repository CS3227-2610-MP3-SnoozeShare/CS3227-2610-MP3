package com.snoozeshare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.snoozeshare.domain.model.TicketCategory;

/** Spring Data JPA repository for ticket categories. */
public interface TicketCategoryRepository
        extends JpaRepository<TicketCategory, UUID> {

    List<TicketCategory> findByActiveTrue();

    @Query("SELECT CASE WHEN COUNT(tc) > 0 THEN true ELSE false END "
            + "FROM TicketCategory tc "
            + "WHERE LOWER(tc.label) = LOWER(:label) "
            + "AND (:excludeId IS NULL OR tc.id <> :excludeId)")
    boolean labelInUse(@Param("label") String label,
                       @Param("excludeId") UUID excludeId);
}
