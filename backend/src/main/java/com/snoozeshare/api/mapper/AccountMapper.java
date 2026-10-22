package com.snoozeshare.api.mapper;

import java.util.List;

import com.snoozeshare.api.response.AccountSummaryResponse;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.service.AccountSummary;

/** Converts accounts to the agent account row. */
public final class AccountMapper {

    private AccountMapper() {
    }

    public static List<AccountSummaryResponse> toResponses(List<AccountSummary> accounts) {
        return accounts.stream().map(AccountSummaryResponse::from).toList();
    }

    public static AccountSummaryResponse toResponse(User user) {
        return AccountSummaryResponse.from(AccountSummary.from(user));
    }
}
