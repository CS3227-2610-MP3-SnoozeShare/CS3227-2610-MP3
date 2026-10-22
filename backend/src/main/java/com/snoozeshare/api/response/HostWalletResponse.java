package com.snoozeshare.api.response;

import java.util.List;

/** Current Host wallet and its complete newest-first statement. */
public record HostWalletResponse(
        WalletResponse wallet,
        List<TransactionResponse> statement) {
}
