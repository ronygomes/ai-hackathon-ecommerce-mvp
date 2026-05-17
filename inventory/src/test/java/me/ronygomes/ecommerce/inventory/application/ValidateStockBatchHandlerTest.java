package me.ronygomes.ecommerce.inventory.application;

import me.rongyomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockBatchValidated;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidateStockBatchHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<InventoryItem, ProductId> repository = mock(Repository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private ValidateStockBatchHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ValidateStockBatchHandler(repository, messageBus);
        when(messageBus.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private static InventoryItem stocked(int qty) {
        return InventoryItem.create(ProductId.generate(), new Quantity(qty));
    }

    @Test
    void handle_allAvailable_publishesStockBatchValidated() throws Exception {
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(50))));

        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(UUID.randomUUID(), 10)))).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement().isInstanceOf(StockBatchValidated.class);
    }

    @Test
    void handle_oneItemUnderstocked_publishesNothing() throws Exception {
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(50))))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(1))));

        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(UUID.randomUUID(), 10),
                new ValidateStockBatchCommand.StockItemRequest(UUID.randomUUID(), 10)))).get();

        verify(messageBus, never()).publish(any());
    }

    @Test
    void handle_itemNotFound_publishesNothing() throws Exception {
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(UUID.randomUUID(), 1)))).get();

        verify(messageBus, never()).publish(any());
    }
}
