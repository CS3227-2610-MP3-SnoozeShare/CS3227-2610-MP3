package com.snoozeshare.service;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.User;

/** Agent account governance: list accounts, suspend with a cascade, reactivate. */
public interface AccountGovernanceService {

    List<AccountSummary> listAccounts();

    User suspend(UUID userId, UUID agentId, String reason);

    User reactivate(UUID userId, UUID agentId, String reason);
}
