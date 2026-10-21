package com.snoozeshare.api.response;

/** Current Host wallet and one bounded page of its statement. */
public record HostWalletResponse(
        WalletResponse wallet,
        PageResponse<TransactionResponse> statement) {
}
