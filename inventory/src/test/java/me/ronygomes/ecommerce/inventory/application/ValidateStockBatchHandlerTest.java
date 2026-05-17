package me.ronygomes.ecommerce.inventory.application;

import me.rongyomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockBatchValidated;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockBatchValidationFailed;
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
    void handle_oneItemUnderstocked_publishesStockBatchValidationFailedWithThatItem() throws Exception {
        UUID outOfStockProductId = UUID.randomUUID();
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(50))))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(1))));

        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(UUID.randomUUID(), 10),
                new ValidateStockBatchCommand.StockItemRequest(outOfStockProductId, 10)))).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(StockBatchValidationFailed.class, failed -> {
                    assertThat(failed.rejected()).singleElement().satisfies(rejected -> {
                        assertThat(rejected.productId()).isEqualTo(outOfStockProductId);
                        assertThat(rejected.requestedQty()).isEqualTo(10);
                        assertThat(rejected.availableQty()).isEqualTo(1);
                        assertThat(rejected.reason()).isEqualTo("Insufficient stock");
                    });
                });
    }

    @Test
    void handle_itemNotFound_publishesStockBatchValidationFailedWithNotFoundReason() throws Exception {
        UUID missingProductId = UUID.randomUUID();
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(missingProductId, 1)))).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(StockBatchValidationFailed.class, failed -> {
                    assertThat(failed.rejected()).singleElement().satisfies(rejected -> {
                        assertThat(rejected.productId()).isEqualTo(missingProductId);
                        assertThat(rejected.availableQty()).isZero();
                        assertThat(rejected.reason()).isEqualTo("Stock item not found");
                    });
                });
    }

    @Test
    void handle_multipleRejections_areAllReportedInOneEvent() throws Exception {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(1))));

        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(a, 5),
                new ValidateStockBatchCommand.StockItemRequest(b, 5)))).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(messageBus).publish(events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(StockBatchValidationFailed.class,
                        failed -> assertThat(failed.rejected()).hasSize(2));
    }
}
