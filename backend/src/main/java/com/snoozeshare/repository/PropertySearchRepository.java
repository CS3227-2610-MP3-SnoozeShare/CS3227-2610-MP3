package com.snoozeshare.repository;

import java.util.List;

import com.snoozeshare.domain.model.Property;

/** Custom fragment for property search queries that cannot be expressed as derived queries. */
public interface PropertySearchRepository {

    List<Property> findBySearchCriteria(SearchCriteria criteria);
}
