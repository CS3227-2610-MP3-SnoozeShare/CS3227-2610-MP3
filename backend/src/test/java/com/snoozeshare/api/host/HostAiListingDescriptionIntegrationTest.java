package com.snoozeshare.api.host;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.snoozeshare.ai.AiFeature;
import com.snoozeshare.ai.LlmCall;
import com.snoozeshare.ai.LlmErrorCode;
import com.snoozeshare.ai.LlmGateway;
import com.snoozeshare.ai.LlmResult;
import com.snoozeshare.ai.host.HostListingDescriptionService;
import com.snoozeshare.ai.host.ListingDescriptionContract;
import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.request.HostAiListingDescriptionRequest;
import com.snoozeshare.api.response.HostAiListingDescriptionResponse;
import com.snoozeshare.api.response.ListingDescriptionAnalysisResponse;
import com.snoozeshare.api.response.ListingDescriptionCriterionResponse;
import com.snoozeshare.api.response.ListingDescriptionModelResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.domain.enums.AmenityType;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.service.ListingService;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class HostAiListingDescriptionIntegrationTest {

    @Mock
    private HostIdentityResolver identityResolver;
    @Mock
    private ListingService listings;
    @Mock
    private LlmGateway gateway;
    @SuppressWarnings("unchecked")
    @Mock
    private AiFeature<ListingDescriptionModelResponse> feature;

    private HostListingDescriptionService service;
    private UUID hostId;
    private FirebaseAuthentication identity;

    @BeforeEach
    void setUp() {
        service = new HostListingDescriptionService(identityResolver, listings, gateway, feature);
        hostId = UUID.randomUUID();
        identity = new FirebaseAuthentication("firebase-host", "host@example.com", true);
        when(identityResolver.resolve(identity)).thenReturn(hostId);
    }

    @Test
    void evaluatesOnlyTheCurrentDescription() {
        HostAiListingDescriptionRequest request = request(
            ListingDescriptionContract.Mode.EVALUATE, "Existing description");
        ListingDescriptionModelResponse model = new ListingDescriptionModelResponse(
            ListingDescriptionContract.Outcome.SUCCESS, ListingDescriptionContract.Mode.EVALUATE,
            analysis(), null, null);
        when(gateway.run(any(), any())).thenReturn(LlmResult.ok(model, false, false, UUID.randomUUID()));

        HostAiListingDescriptionResponse response = service.run(identity, request, "request-1");

        assertThat(response.outcome()).isEqualTo(ListingDescriptionContract.Outcome.SUCCESS);
        assertThat(response.currentAnalysis()).isNotNull();
        assertThat(response.generatedDescription()).isNull();
        assertThat(response.generatedAnalysis()).isNull();
    }

    @Test
    void generatesWithAnEmptyCurrentDescription() {
        HostAiListingDescriptionRequest request = request(
            ListingDescriptionContract.Mode.GENERATE, "");
        String description = "A concise generated description.";
        ListingDescriptionModelResponse model = new ListingDescriptionModelResponse(
            ListingDescriptionContract.Outcome.SUCCESS, ListingDescriptionContract.Mode.GENERATE,
            null, description, analysis());
        when(gateway.run(any(), any())).thenReturn(LlmResult.ok(model, false, false, UUID.randomUUID()));

        HostAiListingDescriptionResponse response = service.run(identity, request, "request-2");

        assertThat(response.generatedDescription()).isEqualTo(description);
        assertThat(response.generatedAnalysis()).isNotNull();
        assertThat(response.outcome()).isEqualTo(ListingDescriptionContract.Outcome.SUCCESS);
    }

    @Test
    void acceptsUsableGenerateResponseWithoutRetiredWordBandAndSerializesFocus() {
        HostAiListingDescriptionRequest request = new HostAiListingDescriptionRequest(
            null, ListingDescriptionContract.Mode.GENERATE,
            ListingDescriptionContract.WritingStyle.PROFESSIONAL,
            ListingDescriptionContract.TargetAudience.ALL_TRAVELLERS,
            ListingDescriptionContract.DescriptionFocus.COMFORT, "", facts());
        ListingDescriptionModelResponse model = new ListingDescriptionModelResponse(
            ListingDescriptionContract.Outcome.SUCCESS, ListingDescriptionContract.Mode.GENERATE,
            null, "Brief factual copy.", analysis());
        when(gateway.run(any(), any())).thenReturn(LlmResult.ok(model, false, false, UUID.randomUUID()));

        HostAiListingDescriptionResponse response = service.run(identity, request, "request-focus");

        assertThat(response.outcome()).isEqualTo(ListingDescriptionContract.Outcome.SUCCESS);
        ArgumentCaptor<LlmCall> call = ArgumentCaptor.forClass(LlmCall.class);
        verify(gateway).run(any(), call.capture());
        assertThat(call.getValue().untrustedContext())
            .contains("descriptionFocus\":\"COMFORT\"", "descriptionLength\":\"50-150\"");
    }

    @Test
    void rejectsBlankEvaluateWithoutCallingGateway() {
        HostAiListingDescriptionRequest request = request(
            ListingDescriptionContract.Mode.EVALUATE, " ");

        assertThatThrownBy(() -> service.run(identity, request, "request-3"))
            .isInstanceOf(HostApiException.class)
            .extracting("code").isEqualTo("DESCRIPTION_REQUIRED_FOR_EVALUATE");
        verify(gateway, never()).run(any(), any());
    }

    @Test
    void rejectsAnotherHostsListingWithoutCallingGateway() {
        UUID listingId = UUID.randomUUID();
        when(listings.getDetail(listingId)).thenReturn(property(UUID.randomUUID()));
        HostAiListingDescriptionRequest request = new HostAiListingDescriptionRequest(
            listingId, ListingDescriptionContract.Mode.GENERATE,
            ListingDescriptionContract.WritingStyle.FRIENDLY,
            ListingDescriptionContract.TargetAudience.ALL_TRAVELLERS,
            ListingDescriptionContract.DescriptionFocus.BALANCED, "", facts());

        assertThatThrownBy(() -> service.run(identity, request, "request-4"))
            .isInstanceOf(HostApiException.class)
            .extracting("code").isEqualTo("NOT_FOUND");
        verify(gateway, never()).run(any(), any());
    }

    @Test
    void mapsGatewayFailureToSafeDegradedResponse() {
        HostAiListingDescriptionRequest request = request(
            ListingDescriptionContract.Mode.GENERATE, "");
        when(gateway.run(any(), any())).thenReturn(LlmResult.degraded(
            new ListingDescriptionModelResponse(ListingDescriptionContract.Outcome.DEGRADED, null, null, null, null),
            LlmErrorCode.DISABLED, false, false, UUID.randomUUID()));

        HostAiListingDescriptionResponse response = service.run(identity, request, "request-5");

        assertThat(response.outcome()).isEqualTo(ListingDescriptionContract.Outcome.DEGRADED);
        assertThat(response.gatewayCode()).isEqualTo("DISABLED");
        assertThat(response.generatedDescription()).isNull();
    }

    @Test
    void doesNotEmitHostDiagnosisLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(HostListingDescriptionService.class);
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
        try {
            HostAiListingDescriptionRequest request = request(
                ListingDescriptionContract.Mode.GENERATE, "");
            when(gateway.run(any(), any())).thenReturn(LlmResult.degraded(
                new ListingDescriptionModelResponse(ListingDescriptionContract.Outcome.DEGRADED,
                    null, null, null, null), LlmErrorCode.DISABLED, false, false, UUID.randomUUID()));
            service.run(identity, request, "request-log-1");

            when(gateway.run(any(), any())).thenReturn(LlmResult.ok(
                new ListingDescriptionModelResponse(ListingDescriptionContract.Outcome.SUCCESS,
                    ListingDescriptionContract.Mode.GENERATE, null, "Generated", null),
                false, false, UUID.randomUUID()));
            service.run(identity, request, "request-log-2");

            assertThat(logs.list).isEmpty();
        } finally {
            logger.detachAppender(logs);
        }
    }

    private HostAiListingDescriptionRequest request(ListingDescriptionContract.Mode mode, String description) {
        return new HostAiListingDescriptionRequest(null, mode,
            ListingDescriptionContract.WritingStyle.FRIENDLY,
            ListingDescriptionContract.TargetAudience.ALL_TRAVELLERS,
            ListingDescriptionContract.DescriptionFocus.BALANCED, description, facts());
    }

    private ListingDescriptionContract.ListingFacts facts() {
        return new ListingDescriptionContract.ListingFacts(
            "Cabin", "HOUSE", "Reno", "NV", 2, 1, 1, List.of("WIFI"), "15:00", "11:00");
    }

    private ListingDescriptionAnalysisResponse analysis() {
        return new ListingDescriptionAnalysisResponse(
            new ListingDescriptionCriterionResponse(ListingDescriptionContract.Status.PASS, "Clear."),
            new ListingDescriptionCriterionResponse(ListingDescriptionContract.Status.PASS, "Complete."),
            new ListingDescriptionCriterionResponse(ListingDescriptionContract.Status.PASS, "Accurate."));
    }

    private Property property(UUID ownerId) {
        return new Property(UUID.randomUUID(), ownerId, ListingStatus.ACTIVE, "Cabin", "Description",
            PropertyType.HOUSE, "1 Main", "Reno", "NV", 89501, 2, 1, 1,
            BigDecimal.valueOf(100), LocalTime.of(15, 0), LocalTime.of(11, 0),
            Set.of(AmenityType.WIFI), Instant.now());
    }
}
