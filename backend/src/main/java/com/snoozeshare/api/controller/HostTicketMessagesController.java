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
import com.snoozeshare.api.response.ConversationSummaryResponse;
import com.snoozeshare.api.response.MessageResponse;
import com.snoozeshare.api.response.PageResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.service.MessageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Host-channel access to agent-managed ticket conversations; Hosts cannot create tickets. */
@RestController
@Validated
@RequestMapping("/api/host/messages/tickets")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostTicketMessagesController {

    private final HostIdentityResolver identityResolver;
    private final MessageService messages;

    public HostTicketMessagesController(HostIdentityResolver identityResolver,
                                        MessageService messages) {
        this.identityResolver = identityResolver;
        this.messages = messages;
    }

    @GetMapping
    public PageResponse<ConversationSummaryResponse> list(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        UUID hostId = identityResolver.resolve(identity);
        try {
            List<ConversationSummaryResponse> items = messages.conversationsFor(
                    hostId, Role.HOST, page, size).stream()
                    .map(ConversationSummaryResponse::from).toList();
            return PageResponse.from(items, page, size);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
    }

    @GetMapping("/{ticketId}")
    public PageResponse<MessageResponse> thread(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID ticketId,
            @RequestParam ThreadChannel channel,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        UUID hostId = identityResolver.resolve(identity);
        requireHostChannel(channel);
        try {
            return PageResponse.from(messages.thread(ticketId, channel, hostId, Role.HOST,
                    page, size).stream().map(MessageResponse::from).toList(), page, size);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
    }

    @PostMapping("/{ticketId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse post(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @PathVariable UUID ticketId,
            @RequestParam ThreadChannel channel,
            @Valid @RequestBody PostMessageRequest request) {
        UUID hostId = identityResolver.resolve(identity);
        requireHostChannel(channel);
        try {
            return MessageResponse.from(messages.post(ticketId, channel, hostId, Role.HOST,
                    request.body()));
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
    }

    @PostMapping("/{ticketId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal FirebaseAuthentication identity,
                         @PathVariable UUID ticketId,
                         @RequestParam ThreadChannel channel) {
        UUID hostId = identityResolver.resolve(identity);
        requireHostChannel(channel);
        try {
            messages.markRead(ticketId, channel, hostId, Role.HOST);
        } catch (IllegalArgumentException exception) {
            throw notFound();
        }
    }

    private void requireHostChannel(ThreadChannel channel) {
        if (channel != ThreadChannel.HOST) {
            throw new HostApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
    }

    private HostApiException notFound() {
        return new HostApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
    }
}
