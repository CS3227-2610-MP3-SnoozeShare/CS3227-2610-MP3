package com.snoozeshare.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import com.snoozeshare.domain.enums.AccountStatus;
import com.snoozeshare.domain.enums.ListingStatus;
import com.snoozeshare.domain.enums.PropertyType;
import com.snoozeshare.domain.enums.Role;
import com.snoozeshare.domain.model.AvailabilityBlock;
import com.snoozeshare.domain.model.Property;
import com.snoozeshare.domain.model.User;
import com.snoozeshare.infra.events.ListingAvailabilityChangedEvent;
import com.snoozeshare.repository.AvailabilityBlockRepository;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.repository.PropertyRepository;
import com.snoozeshare.repository.UserRepository;
import com.snoozeshare.service.AuditService;

class AvailabilityServiceRetryTest {

    private static final UUID HOST_ID = UUID.randomUUID();
    private static final UUID PROPERTY_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void serializationFailureRetriesInANewSerializableTransactionAndPublishesOnce() {
        Fixture fixture = new Fixture();
        TransactionStatus first = mock(TransactionStatus.class);
        TransactionStatus second = mock(TransactionStatus.class);
        when(fixture.transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(first, second);
        doThrow(serializationFailure()).when(fixture.transactions).commit(first);
        fixture.allowBlockSave();

        AvailabilityBlock created = fixture.service.createHostBlock(PROPERTY_ID, START,
            START.plusDays(1), HOST_ID, "Reason");

        assertThat(created.getStartDate()).isEqualTo(START);
        assertThat(created.getEndDate()).isEqualTo(START.plusDays(2));
        verify(fixture.transactions, times(2)).getTransaction(any(TransactionDefinition.class));
        verify(fixture.transactions).commit(second);
        verify(fixture.publisher, times(1)).publishEvent(any(ListingAvailabilityChangedEvent.class));
        ArgumentCaptor<TransactionDefinition> definitions = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(fixture.transactions, times(2)).getTransaction(definitions.capture());
        assertThat(definitions.getAllValues()).allSatisfy(definition -> {
            assertThat(definition.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_SERIALIZABLE);
            assertThat(definition.getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        });
        InOrder commitBeforeEvent = inOrder(fixture.transactions, fixture.publisher);
        commitBeforeEvent.verify(fixture.transactions).commit(second);
        commitBeforeEvent.verify(fixture.publisher).publishEvent(any(ListingAvailabilityChangedEvent.class));
    }

    @Test
    void secondSerializationFailureMapsToStateConflictAndDoesNotPublish() {
        Fixture fixture = new Fixture();
        TransactionStatus first = mock(TransactionStatus.class);
        TransactionStatus second = mock(TransactionStatus.class);
        when(fixture.transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(first, second);
        doThrow(serializationFailure()).when(fixture.transactions).commit(first);
        doThrow(serializationFailure()).when(fixture.transactions).commit(second);
        fixture.allowBlockSave();

        assertThatThrownBy(() -> fixture.service.createHostBlock(PROPERTY_ID, START,
                START.plusDays(1), HOST_ID, "Reason"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Availability changed concurrently");

        verify(fixture.transactions, times(2)).getTransaction(any(TransactionDefinition.class));
        verify(fixture.publisher, times(0)).publishEvent(any(ListingAvailabilityChangedEvent.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void retryThatFindsTheCommittedOverlapReturnsDomainFailure() {
        Fixture fixture = new Fixture();
        TransactionStatus first = mock(TransactionStatus.class);
        TransactionStatus retry = mock(TransactionStatus.class);
        when(fixture.transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(first, retry);
        doThrow(serializationFailure()).when(fixture.transactions).commit(first);
        fixture.allowBlockSave();
        when(fixture.blocks.findOverlapping(PROPERTY_ID, START, START.plusDays(2)))
            .thenReturn(List.of(), List.of(new AvailabilityBlock(UUID.randomUUID(), PROPERTY_ID,
                START, START.plusDays(2), "HOST_BLOCK", null, null)));

        assertThatThrownBy(() -> fixture.service.createHostBlock(PROPERTY_ID, START,
                START.plusDays(1), HOST_ID, "Reason"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Date range is not available");

        verify(fixture.transactions, times(2)).getTransaction(any(TransactionDefinition.class));
        verify(fixture.transactions).rollback(retry);
        verify(fixture.publisher, times(0)).publishEvent(any(ListingAvailabilityChangedEvent.class));
    }

    private TransactionSystemException serializationFailure() {
        return new TransactionSystemException("serialization retry", new SQLException("retry", "40001"));
    }

    private static final class Fixture {
        private final PropertyRepository properties = mock(PropertyRepository.class);
        private final AvailabilityBlockRepository blocks = mock(AvailabilityBlockRepository.class);
        private final BookingRepository bookings = mock(BookingRepository.class);
        private final UserRepository users = mock(UserRepository.class);
        private final AuditService audit = mock(AuditService.class);
        private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        private final AvailabilityServiceImpl service = new AvailabilityServiceImpl(properties, blocks,
            bookings, users, CLOCK, audit, publisher, transactions);

        private Fixture() {
            when(users.findById(HOST_ID)).thenReturn(Optional.of(new User(HOST_ID, "host", Role.HOST,
                "Host", "host@example.test", AccountStatus.ACTIVE, null, CLOCK.instant(), CLOCK.instant(), null)));
            when(properties.findById(PROPERTY_ID)).thenReturn(Optional.of(new Property(PROPERTY_ID, HOST_ID,
                ListingStatus.ACTIVE, "Listing", "Description", PropertyType.APARTMENT,
                "1 Main St", "Singapore", "Central", 123456, 2, 1, 1,
                new BigDecimal("80.00"), LocalTime.NOON, LocalTime.NOON, Set.of(), CLOCK.instant())));
            when(bookings.findOverlapping(PROPERTY_ID, START, START.plusDays(2))).thenReturn(List.of());
            when(blocks.findOverlapping(PROPERTY_ID, START, START.plusDays(2))).thenReturn(List.of());
        }

        private void allowBlockSave() {
            doAnswer(invocation -> invocation.getArgument(0)).when(blocks).save(any(AvailabilityBlock.class));
        }
    }
}
