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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.request.PostMessageRequest;
import com.snoozeshare.api.response.BookingConversationSummaryResponse;
import com.snoozeshare.api.response.MessageResponse;
import com.snoozeshare.api.response.PageResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.BookingMessage;
import com.snoozeshare.service.BookingConversationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
    public PageResponse<BookingConversationSummaryResponse> list(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        UUID hostId = identityResolver.resolve(identity);
        List<BookingConversationSummaryResponse> items;
        try {
            items = conversations.conversationsFor(hostId, Role.HOST, page, size).stream()
                    .map(BookingConversationSummaryResponse::from).toList();
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
        return PageResponse.from(items, page, size);
    }

    @GetMapping("/{bookingId}")
    public PageResponse<MessageResponse> thread(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID bookingId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        UUID hostId = identityResolver.resolve(identity);
        try {
            return messagePage(conversations.thread(bookingId, hostId, Role.HOST, page, size),
                    page, size);
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

    private PageResponse<MessageResponse> messagePage(List<BookingMessage> messages,
                                                       int page, int size) {
        return PageResponse.from(messages.stream().map(MessageResponse::from).toList(), page, size);
    }

    private HostApiException notFound() {
        return new HostApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
}
