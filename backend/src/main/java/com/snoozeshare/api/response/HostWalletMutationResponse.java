package com.snoozeshare.api.response;

/** Result of one mocked Host wallet operation. */
public record HostWalletMutationResponse(
        boolean mocked,
        WalletResponse wallet,
        TransactionResponse transaction) {
}
