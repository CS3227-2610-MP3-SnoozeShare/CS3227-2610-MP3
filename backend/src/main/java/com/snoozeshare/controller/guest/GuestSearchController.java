package com.snoozeshare.controller.guest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.response.PriceBreakdownResponse;
import com.snoozeshare.api.response.PropertyDetailResponse;
import com.snoozeshare.api.response.SearchResultResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.repository.SearchCriteria;
import com.snoozeshare.service.ListingMetrics;
import com.snoozeshare.service.ListingMetricsService;
import com.snoozeshare.service.ListingService;
import com.snoozeshare.service.UserService;

@RestController
@RequestMapping("/api/guest/listings")
@ConditionalOnDatabase
public class GuestSearchController {

    private final ListingService listingService;
    private final ListingMetricsService metricsService;
    private final UserService userService;

    public GuestSearchController(ListingService listingService,
                                 ListingMetricsService metricsService,
                                 UserService userService) {
        this.listingService = listingService;
        this.metricsService = metricsService;
        this.userService = userService;
    }

    @GetMapping
    List<SearchResultResponse> searchListings(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) java.math.BigDecimal maxNightlyRate) {
        SearchCriteria criteria = new SearchCriteria(
                city, guests, startDate, endDate, maxNightlyRate);
        return listingService.search(criteria).stream()
                .map(SearchResultResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    PropertyDetailResponse getDetail(@PathVariable UUID id) {
        Property property = listingService.getDetail(id);
        if (property.getStatus() != ListingStatus.ACTIVE) {
            throw new IllegalArgumentException("Listing not found");
        }
        User host = userService.findById(property.getHostId());
        ListingMetrics metrics = metricsService.metricsFor(property.getId());
        return PropertyDetailResponse.from(property, host, metrics);
    }

    @GetMapping("/{id}/estimate")
    PriceBreakdownResponse estimateCost(
            @PathVariable UUID id,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return PriceBreakdownResponse.from(
                listingService.estimateCost(id, startDate, endDate));
    }
}
