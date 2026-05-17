package me.ronygomes.ecommerce.inventory.application;

import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockDeductedForOrder;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.domain.Quantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeductStockForOrderHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<InventoryItem, ProductId> repository = mock(Repository.class);
    private final MessageBus messageBus = mock(MessageBus.class);
    private DeductStockForOrderHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeductStockForOrderHandler(repository, messageBus);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private static InventoryItem stocked(int qty) {
        return InventoryItem.create(ProductId.generate(), new Quantity(qty));
    }

    @Test
    void handle_deductsEveryItemAndPublishesOneEventPerItem() throws Exception {
        InventoryItem a = stocked(20);
        InventoryItem b = stocked(15);
        a.clearUncommittedEvents();
        b.clearUncommittedEvents();
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(a)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(b)));

        List<List<DomainEvent>> publishedBatches = new ArrayList<>();
        doAnswer(inv -> {
            publishedBatches.add(new ArrayList<>(inv.getArgument(0)));
            return CompletableFuture.completedFuture(null);
        }).when(messageBus).publish(any());

        handler.handle(new DeductStockForOrderCommand(UUID.randomUUID(), List.of(
                new DeductStockForOrderCommand.StockItemRequest(UUID.randomUUID(), 5),
                new DeductStockForOrderCommand.StockItemRequest(UUID.randomUUID(), 7)))).get();

        assertThat(a.getQuantity().value()).isEqualTo(15);
        assertThat(b.getQuantity().value()).isEqualTo(8);
        verify(repository, times(2)).save(any());
        assertThat(publishedBatches).hasSize(2);
        assertThat(publishedBatches.get(0)).singleElement().isInstanceOf(StockDeductedForOrder.class);
        assertThat(publishedBatches.get(1)).singleElement().isInstanceOf(StockDeductedForOrder.class);
    }

    @Test
    void handle_itemNotFound_failsTheFutureWithFailedToDeductMessage() {
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> handler.handle(new DeductStockForOrderCommand(UUID.randomUUID(), List.of(
                new DeductStockForOrderCommand.StockItemRequest(UUID.randomUUID(), 1)))).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to deduct stock");
    }

    @Test
    void handle_insufficientStock_failsTheFuture() {
        InventoryItem item = stocked(2);
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(item)));

        assertThatThrownBy(() -> handler.handle(new DeductStockForOrderCommand(UUID.randomUUID(), List.of(
                new DeductStockForOrderCommand.StockItemRequest(UUID.randomUUID(), 5)))).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to deduct stock");
    }
}
