package com.snoozeshare.api.controller.agent;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.CurrentUserId;
import com.snoozeshare.api.mapper.AccountMapper;
import com.snoozeshare.api.request.AccountActionRequest;
import com.snoozeshare.api.response.AccountSummaryResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.service.AccountGovernanceService;

import jakarta.validation.Valid;

/** Lists Guest and Host accounts and suspends or reactivates them. */
@RestController
@RequestMapping("/api/agent/accounts")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
@ConditionalOnDatabase
public class AgentAccountController {

    private final AccountGovernanceService accounts;

    public AgentAccountController(AccountGovernanceService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public List<AccountSummaryResponse> list() {
        return AccountMapper.toResponses(accounts.listGovernableAccounts());
    }

    @PutMapping("/{userId}/suspend")
    public AccountSummaryResponse suspend(@PathVariable UUID userId, @CurrentUserId UUID agentId,
                                          @Valid @RequestBody AccountActionRequest request) {
        return AccountMapper.toResponse(accounts.suspend(userId, agentId, request.reason()));
    }

    @PutMapping("/{userId}/reactivate")
    public AccountSummaryResponse reactivate(@PathVariable UUID userId, @CurrentUserId UUID agentId,
                                             @Valid @RequestBody AccountActionRequest request) {
        return AccountMapper.toResponse(accounts.reactivate(userId, agentId, request.reason()));
    }
}
