package com.snoozeshare.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.error.HostApiException;
import com.snoozeshare.api.request.WalletAmountRequest;
import com.snoozeshare.api.response.HostWalletMutationResponse;
import com.snoozeshare.api.response.HostWalletResponse;
import com.snoozeshare.api.response.TransactionResponse;
import com.snoozeshare.api.response.WalletResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.model.WalletTransaction;
import com.snoozeshare.service.WalletService;

import jakarta.validation.Valid;

/** Authenticated Host balance and mocked ledger operations. */
@RestController
@Validated
@RequestMapping("/api/host/wallet")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostWalletController {

    private final HostIdentityResolver identityResolver;
    private final WalletService wallets;

    public HostWalletController(HostIdentityResolver identityResolver, WalletService wallets) {
        this.identityResolver = identityResolver;
        this.wallets = wallets;
    }

    @GetMapping
    public HostWalletResponse wallet(
            @AuthenticationPrincipal FirebaseAuthentication identity) {
        UUID hostId = identityResolver.resolve(identity);
        Wallet wallet = requireWallet(hostId);
        List<TransactionResponse> entries = wallets.statementFor(hostId).stream()
                .map(TransactionResponse::from).toList();
        return new HostWalletResponse(WalletResponse.from(wallet), entries);
    }

    @PostMapping("/top-up")
    public HostWalletMutationResponse topUp(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @Valid @RequestBody WalletAmountRequest request) {
        return mutate(identity, request, true);
    }

    @PostMapping("/withdraw")
    public HostWalletMutationResponse withdraw(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @Valid @RequestBody WalletAmountRequest request) {
        return mutate(identity, request, false);
    }

    private HostWalletMutationResponse mutate(FirebaseAuthentication identity,
                                               WalletAmountRequest request,
                                               boolean topUp) {
        UUID hostId = identityResolver.resolve(identity);
        requireWallet(hostId);
        WalletTransaction transaction = topUp
                ? wallets.topUp(hostId, request.amount())
                : wallets.withdraw(hostId, request.amount());
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
