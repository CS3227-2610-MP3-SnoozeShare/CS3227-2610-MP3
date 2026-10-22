package com.snoozeshare.ai.host;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.AuditContext;
import com.snoozeshare.ai.LlmCall;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.LlmGateway;
import com.snoozeshare.ai.LlmResult;
import com.snoozeshare.ai.Turn;
import com.snoozeshare.ai.TurnRole;
import com.snoozeshare.ai.host.ListingDescriptionContract.Mode;
import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.request.HostAiListingDescriptionRequest;
import com.snoozeshare.api.response.HostAiListingDescriptionResponse;
import com.snoozeshare.api.response.ListingDescriptionAnalysisResponse;
import com.snoozeshare.api.response.ListingDescriptionCriterionResponse;
import com.snoozeshare.api.response.ListingDescriptionModelResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.service.ListingService;

/** Orchestrates one authorized Host listing-description assistant request. */
@Service
@ConditionalOnDatabase
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostListingDescriptionService {

    private static final String USER_TASK =
        "Perform the requested listing-description task using the structured data in untrusted context.";
    private final HostIdentityResolver identityResolver;
    private final ListingService listings;
    private final LlmGateway gateway;
    private final AiFeature<ListingDescriptionModelResponse> feature;

    public HostListingDescriptionService(
            HostIdentityResolver identityResolver,
            ListingService listings,
            LlmGateway gateway,
            AiFeature<ListingDescriptionModelResponse> feature) {
        this.identityResolver = identityResolver;
        this.listings = listings;
        this.gateway = gateway;
        this.feature = feature;
    }

    public HostAiListingDescriptionResponse run(
            FirebaseAuthentication identity, HostAiListingDescriptionRequest request, String requestId) {
        UUID hostId = identityResolver.resolve(identity);
        if (request.listingId() != null) {
            requireOwnedListing(request.listingId(), hostId);
        }
        if (request.mode() == Mode.EVALUATE && request.currentDescription().isBlank()) {
            throw new HostApiException(HttpStatus.UNPROCESSABLE_ENTITY, "DESCRIPTION_REQUIRED_FOR_EVALUATE");
        }

        String context = ListingDescriptionContract.serializeContext(
            request.mode().name(), request.writingStyle().name(), request.targetAudience().name(),
            request.length().name(), request.currentDescription(), request.listing());
        LlmCall call = new LlmCall(hostId, com.snoozeshare.domain.enums.Role.HOST,
            List.of(new Turn(TurnRole.USER, USER_TASK)), null, context, AuditContext.NONE);
        LlmResult<ListingDescriptionModelResponse> result = gateway.run(feature, call);
        if (!result.ok()) {
            return HostAiListingDescriptionResponse.degraded(request.mode(), code(result.code()), requestId);
        }
        try {
            validateModel(result.value(), request);
            return HostAiListingDescriptionResponse.from(result.value(), null, requestId);
        } catch (IllegalArgumentException invalidOutput) {
            return HostAiListingDescriptionResponse.degraded(
                request.mode(), LlmErrorCode.INVALID_OUTPUT.name(), requestId);
        }
    }

    private void validateModel(ListingDescriptionModelResponse model, HostAiListingDescriptionRequest request) {
        if (model == null || model.outcome() != ListingDescriptionContract.Outcome.SUCCESS
            || model.mode() != request.mode()) {
            throw new IllegalArgumentException("model response envelope is invalid");
        }
        if (request.mode() == Mode.EVALUATE) {
            requireAnalysis(model.currentAnalysis());
            if (model.generatedDescription() != null || model.generatedAnalysis() != null) {
                throw new IllegalArgumentException("evaluate response contains generated content");
            }
        } else {
            if (!request.currentDescription().isBlank()) {
                requireAnalysis(model.currentAnalysis());
            }
            if (model.generatedDescription() == null || model.generatedDescription().isBlank()) {
                throw new IllegalArgumentException("generate response has no description");
            }
            requireAnalysis(model.generatedAnalysis());
        }
    }

    private void requireAnalysis(ListingDescriptionAnalysisResponse analysis) {
        if (analysis == null) {
            throw new IllegalArgumentException("analysis is required");
        }
        requireCriterion(analysis.clarity());
        requireCriterion(analysis.completeness());
        requireCriterion(analysis.accuracy());
    }

    private void requireCriterion(ListingDescriptionCriterionResponse criterion) {
        if (criterion == null || criterion.status() == null) {
            throw new IllegalArgumentException("analysis criterion is invalid");
        }
        ListingDescriptionContract.requireFeedback(criterion.feedback());
    }

    private Property requireOwnedListing(UUID listingId, UUID hostId) {
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

    private HostApiException notFound() {
        return new HostApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    private String code(LlmErrorCode errorCode) {
        return errorCode == null ? LlmErrorCode.INVALID_OUTPUT.name() : errorCode.name();
    }
}
