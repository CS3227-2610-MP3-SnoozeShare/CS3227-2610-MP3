package com.snoozeshare.service;

import java.util.List;
import java.util.UUID;

import com.snoozeshare.domain.model.User;

/** Agent account governance: list accounts, suspend with a cascade, reactivate. */
public interface AccountGovernanceService {

    /** Guests and Hosts only, oldest first: the accounts an agent may suspend. */
    List<AccountSummary> listGovernableAccounts();

    User suspend(UUID userId, UUID agentId, String reason);

    User reactivate(UUID userId, UUID agentId, String reason);
}
