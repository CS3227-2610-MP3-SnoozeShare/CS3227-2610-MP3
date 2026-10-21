package com.snoozeshare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.snoozeshare.domain.model.Property;

/** Spring Data JPA repository for rental properties. */
public interface PropertyRepository
        extends JpaRepository<Property, UUID>, PropertySearchRepository {

    List<Property> findByHostId(UUID hostId);

    List<Property> findByHostIdOrderByCreatedAtDescIdAsc(UUID hostId, Pageable pageable);
}
