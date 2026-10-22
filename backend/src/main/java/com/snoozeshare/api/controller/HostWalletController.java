package com.snoozeshare.api.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.host.HostWalletViews;
import com.snoozeshare.api.request.WalletAmountRequest;
import com.snoozeshare.api.response.HostWalletMutationResponse;
import com.snoozeshare.api.response.HostWalletResponse;
import com.snoozeshare.auth.security.FirebaseTokenVerifier.FirebaseAuthentication;
import com.snoozeshare.auth.service.HostIdentityResolver;

import jakarta.validation.Valid;

/** Authenticated Host balance and mocked ledger operations. */
@RestController
@Validated
@RequestMapping("/api/host/wallet")
@ConditionalOnProperty(name = "snoozeshare.security.enabled", havingValue = "true")
public class HostWalletController {

    private final HostIdentityResolver identityResolver;
    private final HostWalletViews views;

    public HostWalletController(HostIdentityResolver identityResolver, HostWalletViews views) {
        this.identityResolver = identityResolver;
        this.views = views;
    }

    @GetMapping
    public HostWalletResponse wallet(
            @AuthenticationPrincipal FirebaseAuthentication identity) {
        return views.wallet(identityResolver.resolve(identity));
    }

    @PostMapping("/top-up")
    public HostWalletMutationResponse topUp(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @Valid @RequestBody WalletAmountRequest request) {
        return views.mutate(identityResolver.resolve(identity), request.amount(), true);
    }

    @PostMapping("/withdraw")
    public HostWalletMutationResponse withdraw(
            @AuthenticationPrincipal FirebaseAuthentication identity,
            @Valid @RequestBody WalletAmountRequest request) {
        return views.mutate(identityResolver.resolve(identity), request.amount(), false);
    }
}
