package com.snoozeshare.service.impl;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.snoozeshare.config.ConditionalOnDatabase;
import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.AuditAction;
import com.snoozeshare.domain.enums.BookingStatus;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.AvailabilityBlock;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.domain.policy.StayDatePolicy;
import com.snoozeshare.infra.events.ListingAvailabilityChangedEvent;
import com.snoozeshare.repository.AvailabilityBlockRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.AuditRecord;
import com.snoozeshare.service.AuditService;
import com.snoozeshare.service.AvailabilityConflictException;
import com.snoozeshare.service.AvailabilityService;

@Service
@ConditionalOnDatabase
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final Set<BookingStatus> CALENDAR_STATUSES = EnumSet.of(
            BookingStatus.PENDING, BookingStatus.CONFIRMED,
            BookingStatus.COMPLETED, BookingStatus.FORCE_COMPLETED);

    private final PropertyRepository properties;
    private final AvailabilityBlockRepository blocks;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final StayDatePolicy dates;
    private final Clock clock;
    private final AuditService audit;
    private final ApplicationEventPublisher publisher;
    private final TransactionTemplate serializableTransactions;

    public AvailabilityServiceImpl(PropertyRepository properties,
                                   AvailabilityBlockRepository blocks,
                                   BookingRepository bookings,
                                   UserRepository users,
                                   Clock clock,
                                   AuditService audit,
                                   ApplicationEventPublisher publisher,
                                   PlatformTransactionManager transactionManager) {
        this.properties = properties;
        this.blocks = blocks;
        this.bookings = bookings;
        this.users = users;
        this.clock = clock;
        this.dates = new StayDatePolicy(clock);
        this.audit = audit;
        this.publisher = publisher;
        this.serializableTransactions = new TransactionTemplate(transactionManager);
        this.serializableTransactions.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.serializableTransactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public boolean isRangeAvailable(UUID propertyId, LocalDate start, LocalDate end) {
        return blocks.findOverlapping(propertyId, start, end).isEmpty()
                && bookings.findOverlapping(propertyId, start, end).isEmpty();
    }

    @Override
    public AvailabilityBlock createHostBlock(UUID propertyId, LocalDate start, LocalDate end,
                                              UUID hostId, String reason) {
        AvailabilityBlock created = runSerializable(() -> {
            requireValidBlock(propertyId, start, end);
            User host = requireActiveHost(hostId);
            LocalDate exclusiveEnd = end.plusDays(1);
            Property property = properties.findById(propertyId)
                    .orElseThrow(() -> new IllegalArgumentException("Property does not exist"));
            if (!property.getHostId().equals(host.getId())) {
                throw new IllegalStateException("Host does not own this property");
            }
            if (!bookings.findOverlapping(propertyId, start, exclusiveEnd).isEmpty()
                    || !blocks.findOverlapping(propertyId, start, exclusiveEnd).isEmpty()) {
                throw new AvailabilityConflictException();
            }
            String normalizedReason = reason == null ? null : reason.trim();
            if (normalizedReason != null && normalizedReason.isEmpty()) {
                normalizedReason = null;
            }
            AvailabilityBlock saved = blocks.save(new AvailabilityBlock(UUID.randomUUID(), propertyId,
                    start, exclusiveEnd, "HOST_BLOCK", null, normalizedReason));
            audit.record(AuditRecord.builder(host.getId(), AuditAction.HOST_BLOCK_CREATED,
                            "AvailabilityBlock", saved.getId())
                    .subject(host.getId())
                    .reason(blockAuditReason("created", start, end, normalizedReason))
                    .build());
            return saved;
        });
        publisher.publishEvent(new ListingAvailabilityChangedEvent(propertyId, hostId, clock.instant()));
        return created;
    }

    @Override
    public void removeHostBlock(UUID blockId, UUID hostId) {
        UUID propertyId = runSerializable(() -> {
            User host = requireActiveHost(hostId);
            AvailabilityBlock block = blocks.findById(blockId)
                    .orElseThrow(() -> new EmptyResultDataAccessException("Availability block not found", 1));
            if (!"HOST_BLOCK".equals(block.getSource())) {
                throw new IllegalStateException("Booking blocks cannot be removed");
            }
            Property property = properties.findById(block.getPropertyId())
                    .orElseThrow(() -> new EmptyResultDataAccessException("Property not found", 1));
            if (!property.getHostId().equals(host.getId())) {
                throw new IllegalStateException("Host does not own this property");
            }
            audit.record(AuditRecord.builder(host.getId(), AuditAction.HOST_BLOCK_REMOVED,
                            "AvailabilityBlock", block.getId())
                    .subject(host.getId())
                    .reason(blockAuditReason("removed", block.getStartDate(),
                            block.getEndDate().minusDays(1), block.getReason()))
                    .build());
            blocks.deleteById(blockId);
            return block.getPropertyId();
        });
        publisher.publishEvent(new ListingAvailabilityChangedEvent(propertyId, hostId, clock.instant()));
    }

    @Override
    public AvailabilityBlock getBlock(UUID blockId) {
        return blocks.findById(blockId)
                .orElseThrow(() -> new EmptyResultDataAccessException("Availability block not found", 1));
    }

    @Override
    public List<AvailabilityBlock> blocksFor(UUID propertyId, LocalDate start, LocalDate endExclusive) {
        List<AvailabilityBlock> stored = blocks
                .findByPropertyIdAndStartDateLessThanAndEndDateGreaterThanOrderByStartDateAscIdAsc(
                        propertyId, endExclusive, start);
        Set<UUID> covered = stored.stream().map(AvailabilityBlock::getBookingId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<AvailabilityBlock> shown = new ArrayList<>(stored);
        bookings.findByListingIdAndStatusInAndStartDateLessThanAndEndDateGreaterThanOrderByStartDateAscIdAsc(
                propertyId, List.copyOf(CALENDAR_STATUSES), endExclusive, start).stream()
            .filter(booking -> !covered.contains(booking.getId()))
            .map(booking -> new AvailabilityBlock(UUID.randomUUID(), propertyId,
                    booking.getStartDate(), booking.getEndDate(), "BOOKING", booking.getId(), null))
            .forEach(shown::add);
        return shown.stream().sorted((left, right) -> {
            int byStart = left.getStartDate().compareTo(right.getStartDate());
            return byStart != 0 ? byStart : left.getId().compareTo(right.getId());
        }).toList();
    }

    @Override
    public List<AvailabilityBlock> blocksFor(UUID propertyId) {
        List<AvailabilityBlock> stored = blocks.findByPropertyId(propertyId);
        Set<UUID> covered = stored.stream().map(AvailabilityBlock::getBookingId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<AvailabilityBlock> shown = new ArrayList<>(stored);
        for (Booking booking : bookings.findByListingId(propertyId)) {
            if (CALENDAR_STATUSES.contains(booking.getStatus())
                    && !covered.contains(booking.getId())) {
                shown.add(new AvailabilityBlock(UUID.randomUUID(), propertyId,
                        booking.getStartDate(), booking.getEndDate(), "BOOKING",
                        booking.getId(), null));
            }
        }
        return shown;
    }

    private User requireActiveHost(UUID hostId) {
        if (hostId == null) {
            throw new IllegalStateException("Host ID must not be null");
        }
        User host = users.findById(hostId)
                .orElseThrow(() -> new IllegalArgumentException("Host does not exist"));
        if (host.getRole() != Role.HOST) {
            throw new IllegalArgumentException("Only hosts can manage calendar blocks");
        }
        if (host.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Host account is not active");
        }
        return host;
    }

    private void requireValidBlock(UUID propertyId, LocalDate start, LocalDate end) {
        if (propertyId == null || start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Property and a valid date range are required");
        }
        dates.requireNotInPast(start, "From date");
    }

    private <T> T runSerializable(Supplier<T> operation) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return serializableTransactions.execute(status -> operation.get());
            } catch (RuntimeException exception) {
                if (!isSerializationFailure(exception)) {
                    throw exception;
                }
                if (attempt == 1) {
                    throw new IllegalStateException("Availability changed concurrently", exception);
                }
            }
        }
        throw new IllegalStateException("Availability transaction retry exhausted");
    }

    private boolean isSerializationFailure(Throwable failure) {
        Set<Throwable> seen = new HashSet<>();
        for (Throwable current = failure; current != null && seen.add(current); current = current.getCause()) {
            if (current instanceof SQLException sqlException && "40001".equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private String blockAuditReason(String action, LocalDate start, LocalDate end, String reason) {
        String range = start + " through " + end;
        return reason == null ? "Host block " + action + ": " + range
                : "Host block " + action + ": " + range + " — " + reason;
    }
}
