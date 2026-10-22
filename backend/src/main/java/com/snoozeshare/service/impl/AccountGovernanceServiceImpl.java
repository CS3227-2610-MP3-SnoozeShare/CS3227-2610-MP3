package com.snoozeshare.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.enums.WalletTransactionType;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.model.Wallet;
import com.snoozeshare.domain.statemachine.BookingStateMachine;
import com.snoozeshare.infra.events.AccountStatusChangedEvent;
import com.snoozeshare.infra.events.BookingCancelledEvent;
import com.snoozeshare.repository.AvailabilityBlockRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.repository.WalletRepository;
import com.snoozeshare.service.AccountGovernanceService;
import com.snoozeshare.service.AccountSummary;
import com.snoozeshare.service.AuditRecord;
import com.snoozeshare.service.AuditService;

@Service

public class AccountGovernanceServiceImpl implements AccountGovernanceService {

    static final String CASCADE_BOOKING_REASON = "Account suspended — cascading cancellation";
    static final String CASCADE_LISTING_REASON = "Host suspended";

    private final UserRepository users;
    private final BookingRepository bookings;
    private final PropertyRepository properties;
    private final AvailabilityBlockRepository blocks;
    private final WalletRepository wallets;
    private final LedgerWriter ledger;
    private final ApplicationEventPublisher publisher;
    private final AuditService audit;
    private final Clock clock;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public AccountGovernanceServiceImpl(UserRepository users, BookingRepository bookings,
                                        PropertyRepository properties,
                                        AvailabilityBlockRepository blocks,
                                        WalletRepository wallets, LedgerWriter ledger,
                                        ApplicationEventPublisher publisher,
                                        AuditService audit, Clock clock) {
        this.users = users;
        this.bookings = bookings;
        this.properties = properties;
        this.blocks = blocks;
        this.wallets = wallets;
        this.ledger = ledger;
        this.publisher = publisher;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public List<AccountSummary> listAccounts() {
        return users.findAllByOrderByCreatedAtAscIdAsc().stream()
                .filter(user -> user.getRole() != Role.SYSTEM)
                .map(AccountSummary::from)
                .toList();
    }

    @Override
    @Transactional
    public User suspend(UUID userId, UUID agentId, String reason) {
        String text = requireReason(reason);
        requireActiveAgent(agentId);
        User target = requireGovernable(userId);
        if (target.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is already suspended");
        }
        Instant now = clock.instant();
        target.setStatus(AccountStatus.SUSPENDED);
        target.setSuspensionReason(text);
        target.setUpdatedAt(now);
        User saved = users.save(target);
        audit.record(AuditRecord.builder(agentId, AuditAction.ACCOUNT_SUSPENDED, "User", userId)
                .status(AccountStatus.ACTIVE, AccountStatus.SUSPENDED).reason(text)
                .subject(userId).at(now).build());
        cascade(target, agentId, now);
        publisher.publishEvent(new AccountStatusChangedEvent(userId, AccountStatus.SUSPENDED,
                agentId, clock.instant()));
        return saved;
    }

    @Override
    @Transactional
    public User reactivate(UUID userId, UUID agentId, String reason) {
        String text = requireReason(reason);
        requireActiveAgent(agentId);
        User target = requireGovernable(userId);
        if (target.getStatus() != AccountStatus.SUSPENDED) {
            throw new IllegalStateException("Account is not suspended");
        }
        Instant now = clock.instant();
        target.setStatus(AccountStatus.ACTIVE);
        target.setSuspensionReason(null);
        target.setUpdatedAt(now);
        User saved = users.save(target);
        audit.record(AuditRecord.builder(agentId, AuditAction.ACCOUNT_REACTIVATED, "User",
                        userId)
                .status(AccountStatus.SUSPENDED, AccountStatus.ACTIVE).reason(text)
                .subject(userId).at(now).build());
        publisher.publishEvent(new AccountStatusChangedEvent(userId, AccountStatus.ACTIVE,
                agentId, clock.instant()));
        return saved;
    }

    private void cascade(User target, UUID agentId, Instant now) {
        LocalDate today = LocalDate.ofInstant(now, clock.getZone());
        if (target.getRole() == Role.GUEST) {
            for (Booking booking : bookings.findByGuestId(target.getId())) {
                if (isCancellable(booking, today)) {
                    forceCancel(booking, target, agentId, now);
                }
            }
        } else {
            for (Property property : properties.findByHostId(target.getId())) {
                for (Booking booking : bookings.findByListingId(property.getId())) {
                    if (isCancellable(booking, today)) {
                        forceCancel(booking, target, agentId, now);
                    }
                }
                if (property.getStatus() == ListingStatus.ACTIVE) {
                    deactivate(property, target, agentId, now);
                }
            }
        }
    }

    private void deactivate(Property property, User host, UUID agentId, Instant now) {
        property.setStatus(ListingStatus.INACTIVE);
        properties.save(property);
        audit.record(AuditRecord.builder(agentId, AuditAction.LISTING_STATUS_CASCADE,
                        "Property", property.getId())
                .status(ListingStatus.ACTIVE, ListingStatus.INACTIVE)
                .reason(CASCADE_LISTING_REASON).subject(host.getId()).at(now).build());
    }

    static boolean isCancellable(Booking booking, LocalDate today) {
        return booking.getStatus() == BookingStatus.PENDING
                || (booking.getStatus() == BookingStatus.CONFIRMED
                        && booking.getStartDate().isAfter(today));
    }

    private void forceCancel(Booking booking, User suspended, UUID agentId, Instant now) {
        BookingStatus before = booking.getStatus();
        if (!BookingStateMachine.canTransition(before,
                BookingStatus.FORCE_CANCELLED, Role.AGENT)) {
            throw new IllegalStateException("Cannot force-cancel a booking in status "
                    + before);
        }
        booking.setStatus(BookingStatus.FORCE_CANCELLED);
        booking.setDecidedAt(now);
        bookings.save(booking);
        blocks.deleteByBookingId(booking.getId());
        audit.record(AuditRecord.builder(agentId, AuditAction.BOOKING_FORCE_CANCELLED,
                        "Booking", booking.getId())
                .status(before, BookingStatus.FORCE_CANCELLED)
                .reason(CASCADE_BOOKING_REASON).subject(suspended.getId())
                .booking(booking.getId()).at(now).build());
        Wallet wallet = wallets.findByUserId(booking.getGuestId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Guest wallet does not exist"));
        ledger.post(wallet.getId(), WalletTransactionType.ESCROW_REFUND,
                booking.getTotalAmount(), agentId, booking.getId(), null, null, now);
        publisher.publishEvent(new BookingCancelledEvent(booking.getId(), agentId, now));
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is required");
        }
        return reason.trim();
    }

    private void requireActiveAgent(UUID agentId) {
        User agent = users.findById(agentId)
                .orElseThrow(() -> new IllegalArgumentException("Agent does not exist"));
        if (agent.getRole() != Role.AGENT) {
            throw new IllegalArgumentException("Only a support agent can manage accounts");
        }
        if (agent.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
    }

    private User requireGovernable(UUID userId) {
        User target = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        if (target.getRole() == Role.AGENT || target.getRole() == Role.SYSTEM) {
            throw new IllegalArgumentException(
                    "Support agent accounts cannot be suspended or reactivated");
        }
        return target;
    }
}
