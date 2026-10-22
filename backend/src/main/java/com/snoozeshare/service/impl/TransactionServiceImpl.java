package com.snoozeshare.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.WalletTransactionType;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.model.WalletTransaction;
import com.snoozeshare.domain.settlement.SettlementCalculator;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.LedgerRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.WalletRepository;
import com.snoozeshare.service.TransactionService;

@Service
@ConditionalOnDatabase
public class TransactionServiceImpl implements TransactionService {

    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final WalletRepository wallets;
    private final LedgerWriter ledger;
    private final LedgerRepository ledgerEntries;

    public TransactionServiceImpl(BookingRepository bookings,
                                  PropertyRepository properties,
                                  WalletRepository wallets,
                                  LedgerWriter ledger,
                                  LedgerRepository ledgerEntries) {
        this.bookings = bookings;
        this.properties = properties;
        this.wallets = wallets;
        this.ledger = ledger;
        this.ledgerEntries = ledgerEntries;
    }

    @Override
    @Transactional
    public WalletTransaction holdEscrow(UUID bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        return ledger.post(guestWallet(booking).getId(),
                WalletTransactionType.ESCROW_HOLD, booking.getTotalAmount().negate(),
                booking.getGuestId(), bookingId, null, null, Instant.now());
    }

    @Override
    @Transactional
    public WalletTransaction refundEscrow(UUID bookingId, BigDecimal refundAmount) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        return ledger.post(guestWallet(booking).getId(),
                WalletTransactionType.ESCROW_REFUND, refundAmount, booking.getGuestId(),
                bookingId, null, null, Instant.now());
    }

    @Override
    @Transactional
    public WalletTransaction settleBookingCompletion(UUID bookingId) {
        Booking booking = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking does not exist"));
        var existing = ledgerEntries.entriesForBooking(bookingId).stream()
                .filter(txn -> txn.actionType().equals(WalletTransactionType.BOOKING_PAYOUT.name()))
                .findFirst();
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            return existing.orElseThrow(() ->
                    new IllegalStateException("Completed booking has no payout"));
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be completed");
        }
        if (existing.isPresent()) {
            return existing.get();
        }
        Property property = properties.findById(booking.getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
        Wallet hostWallet = wallets.findByUserId(property.getHostId())
                .orElseThrow(() -> new IllegalArgumentException("Host wallet does not exist"));
        BigDecimal gross = booking.getTotalAmount();
        BigDecimal net = SettlementCalculator.hostNet(gross);
        BigDecimal fee = SettlementCalculator.platformFee(gross);
        Instant now = Instant.now();
        WalletTransaction result = ledger.postPayout(hostWallet.getId(), net, fee,
                property.getHostId(), bookingId, null, now);
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(now);
        bookings.save(booking);
        return result;
    }

    @Override
    public List<WalletTransaction> historyFor(UUID bookingId) {
        return ledgerEntries.entriesForBooking(bookingId);
    }

    private Wallet guestWallet(Booking booking) {
        return wallets.findByUserId(booking.getGuestId())
                .orElseThrow(() -> new IllegalArgumentException("Guest wallet does not exist"));
    }
}
