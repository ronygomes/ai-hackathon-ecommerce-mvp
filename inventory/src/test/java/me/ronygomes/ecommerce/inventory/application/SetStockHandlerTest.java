package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.inventory.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SetStockHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<InventoryItem, ProductId> repository = mock(Repository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private SetStockHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SetStockHandler(repository, outboxStore);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private AtomicReference<List<DomainEvent>> captureAppendedEvents(AtomicReference<String> aggIdRef) {
        AtomicReference<List<DomainEvent>> ref = new AtomicReference<>();
        doAnswer(inv -> {
            if (aggIdRef != null) aggIdRef.set(inv.getArgument(0));
            ref.set(new ArrayList<>(inv.getArgument(1)));
            return null;
        }).when(outboxStore).append(any(), any());
        return ref;
    }

    @Test
    void handle_existingItem_updatesQuantityAndAppendsOnlyStockSet() throws Exception {
        ProductId pid = ProductId.generate();
        InventoryItem existing = InventoryItem.create(pid, new Quantity(10));
        existing.clearUncommittedEvents();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(existing)));
        AtomicReference<String> aggId = new AtomicReference<>();
        AtomicReference<List<DomainEvent>> appended = captureAppendedEvents(aggId);

        handler.handle(new SetStockCommand(pid.value(), 25, "restock")).get();

        assertThat(existing.getQuantity().value()).isEqualTo(25);
        assertThat(aggId.get()).isEqualTo(pid.toString());
        assertThat(appended.get()).singleElement().isInstanceOf(StockSet.class);
        verify(repository).save(existing);
    }

    @Test
    void handle_missingItem_createsZeroQtyAggregateThenSetsStock() throws Exception {
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        AtomicReference<List<DomainEvent>> appended = captureAppendedEvents(null);

        handler.handle(new SetStockCommand(UUID.randomUUID(), 5, "initial")).get();

        ArgumentCaptor<InventoryItem> saved = ArgumentCaptor.forClass(InventoryItem.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getQuantity().value()).isEqualTo(5);
        assertThat(appended.get()).hasSize(2)
                .anyMatch(StockItemCreated.class::isInstance)
                .anyMatch(StockSet.class::isInstance);
    }
}
