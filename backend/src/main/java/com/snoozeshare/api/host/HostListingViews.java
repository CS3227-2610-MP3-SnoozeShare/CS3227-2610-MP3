package com.snoozeshare.api.host;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.mapper.ListingMapper;
import com.snoozeshare.api.request.CreateListingRequest;
import com.snoozeshare.api.request.UpdateListingRequest;
import com.snoozeshare.api.response.HostListingDetailResponse;
import com.snoozeshare.api.response.HostListingSummaryResponse;
import com.snoozeshare.api.response.ListingMetricsResponse;
import com.snoozeshare.api.response.PropertyResponse;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.service.ListingMetricsService;
import com.snoozeshare.service.ListingService;
import com.snoozeshare.service.ListingSummaryMetrics;

/**
 * Host listing reads and writes as response DTOs, with the ownership rule: a listing that does not exist and a
 * listing owned by another host are both a 404. Keeps domain entities out of the Host controllers.
 */
@Component
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostListingViews {

    private final ListingService listings;
    private final ListingMetricsService metrics;

    public HostListingViews(ListingService listings, ListingMetricsService metrics) {
        this.listings = listings;
        this.metrics = metrics;
    }

    /** Throws a 404 unless the host owns the listing. */
    public void requireOwned(UUID listingId, UUID hostId) {
        owned(listingId, hostId);
    }

    public List<HostListingSummaryResponse> summaries(UUID hostId) {
        List<Property> properties = listings.findByHostId(hostId);
        if (properties.isEmpty()) {
            return List.of();
        }
        Map<UUID, ListingSummaryMetrics> summaries = metrics.summaryMetricsForHost(hostId);
        return properties.stream()
            .map(property -> HostListingSummaryResponse.from(property,
                summaries.getOrDefault(property.getId(), new ListingSummaryMetrics(0, 0.0))))
            .toList();
    }

    public HostListingDetailResponse detail(UUID listingId, UUID hostId) {
        Property property = owned(listingId, hostId);
        return new HostListingDetailResponse(
            PropertyResponse.from(property), ListingMetricsResponse.from(metrics.metricsFor(property.getId())));
    }

    public PropertyResponse create(CreateListingRequest request, UUID hostId) {
        return PropertyResponse.from(listings.create(ListingMapper.toDraft(request), hostId));
    }

    public PropertyResponse update(UUID listingId, UpdateListingRequest request, UUID hostId) {
        owned(listingId, hostId);
        return PropertyResponse.from(listings.update(ListingMapper.toDraft(listingId, request), hostId));
    }

    public PropertyResponse updateStatus(UUID listingId, ListingStatus status, UUID hostId) {
        owned(listingId, hostId);
        return PropertyResponse.from(listings.updateStatus(listingId, status, hostId));
    }

    private Property owned(UUID listingId, UUID hostId) {
        Property property;
        try {
            property = listings.getDetail(listingId);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
        if (!hostId.equals(property.getHostId())) {
            throw notFound();
        }
        return property;
    }

    private static HostApiException notFound() {
        return new HostApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
}
