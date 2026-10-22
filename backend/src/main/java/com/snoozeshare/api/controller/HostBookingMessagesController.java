package com.snoozeshare.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.request.PostMessageRequest;
import com.snoozeshare.api.response.BookingConversationSummaryResponse;
import com.snoozeshare.api.response.MessageResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.service.BookingConversationService;

import jakarta.validation.Valid;

/** Host access to participant-scoped booking conversations. */
@RestController
@Validated
@RequestMapping("/api/host/messages/bookings")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostBookingMessagesController {

    private final HostIdentityResolver identityResolver;
    private final BookingConversationService conversations;

    public HostBookingMessagesController(HostIdentityResolver identityResolver,
                                        BookingConversationService conversations) {
        this.identityResolver = identityResolver;
        this.conversations = conversations;
    }

    @GetMapping
    public List<BookingConversationSummaryResponse> list(
            @AuthenticationPrincipal FirebaseAuthentication identity) {
        UUID hostId = identityResolver.resolve(identity);
        List<BookingConversationSummaryResponse> items;
        try {
            items = conversations.conversationsFor(hostId, Role.HOST).stream()
                    .map(BookingConversationSummaryResponse::from).toList();
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
        return items;
    }

    @GetMapping("/{bookingId}")
    public List<MessageResponse> thread(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID bookingId) {
        UUID hostId = identityResolver.resolve(identity);
        try {
            return conversations.thread(bookingId, hostId, Role.HOST).stream()
                    .map(MessageResponse::from).toList();
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
    }

    @PostMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse post(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID bookingId,
            @Valid @RequestBody PostMessageRequest request) {
        UUID hostId = identityResolver.resolve(identity);
        try {
            return MessageResponse.from(conversations.post(bookingId, hostId, Role.HOST,
                    request.body()));
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
    }

    @PostMapping("/{bookingId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal FirebaseAuthentication identity,
                         @PathVariable UUID bookingId) {
        UUID hostId = identityResolver.resolve(identity);
        try {
            conversations.markRead(bookingId, hostId, Role.HOST);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
    }

    private HostApiException notFound() {
        return new HostApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
}
