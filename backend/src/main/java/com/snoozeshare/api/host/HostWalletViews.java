package com.snoozeshare.api.host;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.response.HostWalletMutationResponse;
import com.snoozeshare.api.response.HostWalletResponse;
import com.snoozeshare.api.response.TransactionResponse;
import com.snoozeshare.api.response.WalletResponse;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.model.WalletTransaction;
import com.snoozeshare.service.WalletService;

/** Host balance, statement and mocked top-up or withdrawal as response DTOs. */
@Component
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostWalletViews {

    private final WalletService wallets;

    public HostWalletViews(WalletService wallets) {
        this.wallets = wallets;
    }

    public HostWalletResponse wallet(UUID hostId) {
        Wallet wallet = requireWallet(hostId);
        List<TransactionResponse> entries = wallets.statementFor(hostId).stream()
                .map(TransactionResponse::from).toList();
        return new HostWalletResponse(WalletResponse.from(wallet), entries);
    }

    public HostWalletMutationResponse mutate(UUID hostId, BigDecimal amount, boolean topUp) {
        requireWallet(hostId);
        WalletTransaction transaction = topUp ? wallets.topUp(hostId, amount) : wallets.withdraw(hostId, amount);
        return new HostWalletMutationResponse(true, WalletResponse.from(requireWallet(hostId)),
                TransactionResponse.from(transaction));
    }

    private Wallet requireWallet(UUID hostId) {
        try {
            return wallets.getWallet(hostId);
        } catch (IllegalArgumentException exception) {
            throw new HostApiException(HttpStatus.NOT_FOUND, "NOT_FOUND");
        }
    }
}
