package com.snoozeshare.repository;

import java.util.ArrayList;
import java.util.List;

import com.snoozeshare.domain.model.Property;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/** JPA Criteria-free implementation using JPQL with dynamic predicates. */
public class PropertySearchRepositoryImpl implements PropertySearchRepository {

    private final EntityManager em;

    public PropertySearchRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Property> findBySearchCriteria(SearchCriteria criteria) {
        var jpql = new StringBuilder(
                "SELECT p FROM Property p WHERE p.status = 'ACTIVE'");
        var params = new ArrayList<Object[]>();

        if (criteria.city() != null && !criteria.city().isBlank()) {
            jpql.append(" AND LOWER(p.city) LIKE LOWER(:city)");
            params.add(new Object[]{"city", "%" + criteria.city().trim() + "%"});
        }
        if (criteria.guests() != null) {
            jpql.append(" AND p.maxGuests >= :guests");
            params.add(new Object[]{"guests", criteria.guests()});
        }
        if (criteria.maxNightlyRate() != null) {
            jpql.append(" AND p.baseNightlyRate <= :maxRate");
            params.add(new Object[]{"maxRate", criteria.maxNightlyRate()});
        }
        if (criteria.startDate() != null && criteria.endDate() != null) {
            jpql.append(" AND NOT EXISTS ("
                    + "SELECT ab FROM AvailabilityBlock ab "
                    + "WHERE ab.propertyId = p.id "
                    + "AND ab.startDate < :endDate "
                    + "AND ab.endDate > :startDate)");
            params.add(new Object[]{"startDate", criteria.startDate()});
            params.add(new Object[]{"endDate", criteria.endDate()});
        }

        TypedQuery<Property> query = em.createQuery(
                jpql.toString(), Property.class);
        for (Object[] param : params) {
            query.setParameter((String) param[0], param[1]);
        }
        return query.getResultList();
    }
}
