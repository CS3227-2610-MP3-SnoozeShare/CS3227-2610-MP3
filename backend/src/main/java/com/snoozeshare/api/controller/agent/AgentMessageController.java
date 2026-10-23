package com.snoozeshare.api.controller.agent;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.CurrentUserId;
import com.snoozeshare.api.mapper.MessageMapper;
import com.snoozeshare.api.request.PostMessageRequest;
import com.snoozeshare.api.response.MessageResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.ThreadChannel;
import com.snoozeshare.service.MessageService;

import jakarta.validation.Valid;

/** The two chat threads (guest and host) of a dispute ticket, readable and writable by any agent. */
@RestController
@RequestMapping("/api/agent/disputes/{ticketId}/messages/{channel}")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
@ConditionalOnDatabase
public class AgentMessageController {

    private final MessageService messages;

    public AgentMessageController(MessageService messages) {
        this.messages = messages;
    }

    @GetMapping
    public List<MessageResponse> thread(@PathVariable UUID ticketId, @PathVariable ThreadChannel channel,
                                        @CurrentUserId UUID agentId) {
        return MessageMapper.toResponses(messages.thread(ticketId, channel, agentId, Role.AGENT));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse post(@PathVariable UUID ticketId, @PathVariable ThreadChannel channel,
                                @Valid @RequestBody PostMessageRequest request, @CurrentUserId UUID agentId) {
        return MessageMapper.toResponse(messages.post(ticketId, channel, agentId, Role.AGENT, request.body()));
    }
}
