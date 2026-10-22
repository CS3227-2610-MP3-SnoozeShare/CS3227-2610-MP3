package com.snoozeshare.controller.guest;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.request.WalletAmountRequest;
import com.snoozeshare.api.response.TransactionResponse;
import com.snoozeshare.api.response.WalletResponse;
import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.service.WalletService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/guest/wallet")
@ConditionalOnDatabase
public class GuestWalletController {

    private final WalletService walletService;

    public GuestWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    WalletResponse getWallet(HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        return WalletResponse.from(walletService.getWallet(guestId));
    }

    @GetMapping("/transactions")
    List<TransactionResponse> getTransactions(HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        return walletService.statementFor(guestId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @PostMapping("/topup")
    WalletResponse topUp(@Valid @RequestBody WalletAmountRequest req,
                         HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        walletService.topUp(guestId, req.amount());
        return WalletResponse.from(walletService.getWallet(guestId));
    }

    @PostMapping("/withdraw")
    WalletResponse withdraw(@Valid @RequestBody WalletAmountRequest req,
                            HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        walletService.withdraw(guestId, req.amount());
        return WalletResponse.from(walletService.getWallet(guestId));
    }
}
