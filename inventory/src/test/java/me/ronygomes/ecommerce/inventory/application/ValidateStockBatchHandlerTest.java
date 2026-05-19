package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockBatchValidated;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockBatchValidationFailed;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
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
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private ValidateStockBatchHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ValidateStockBatchHandler(repository, outboxStore);
    }

    private static InventoryItem stocked(int qty) {
        return InventoryItem.create(ProductId.generate(), new Quantity(qty));
    }

    @Test
    void handle_allAvailable_appendsStockBatchValidatedToOutbox() throws Exception {
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(50))));

        UUID correlationId = UUID.randomUUID();
        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(UUID.randomUUID(), 10)), correlationId, "test-cause")).get();

        ArgumentCaptor<String> aggIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<DomainEvent>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(outboxStore).append(aggIdCaptor.capture(), eventsCaptor.capture());
        assertThat(aggIdCaptor.getValue()).isNotBlank();
        assertThat(eventsCaptor.getValue()).singleElement()
                .isInstanceOfSatisfying(StockBatchValidated.class,
                        e -> assertThat(e.correlationId()).isEqualTo(correlationId));
    }

    @Test
    void handle_oneItemUnderstocked_appendsStockBatchValidationFailedWithThatItem() throws Exception {
        UUID outOfStockProductId = UUID.randomUUID();
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(50))))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(stocked(1))));

        UUID correlationId = UUID.randomUUID();
        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(UUID.randomUUID(), 10),
                new ValidateStockBatchCommand.StockItemRequest(outOfStockProductId, 10)), correlationId, "test-cause")).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(outboxStore).append(any(), events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(StockBatchValidationFailed.class, failed -> {
                    assertThat(failed.correlationId()).isEqualTo(correlationId);
                    assertThat(failed.rejected()).singleElement().satisfies(rejected -> {
                        assertThat(rejected.productId()).isEqualTo(outOfStockProductId);
                        assertThat(rejected.requestedQty()).isEqualTo(10);
                        assertThat(rejected.availableQty()).isEqualTo(1);
                        assertThat(rejected.reason()).isEqualTo("Insufficient stock");
                    });
                });
    }

    @Test
    void handle_itemNotFound_appendsStockBatchValidationFailedWithNotFoundReason() throws Exception {
        UUID missingProductId = UUID.randomUUID();
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        handler.handle(new ValidateStockBatchCommand(List.of(
                new ValidateStockBatchCommand.StockItemRequest(missingProductId, 1)), UUID.randomUUID(), "test-cause")).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(outboxStore).append(any(), events.capture());
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
                new ValidateStockBatchCommand.StockItemRequest(b, 5)), UUID.randomUUID(), "test-cause")).get();

        ArgumentCaptor<List<DomainEvent>> events = ArgumentCaptor.forClass(List.class);
        verify(outboxStore).append(any(), events.capture());
        assertThat(events.getValue()).singleElement()
                .isInstanceOfSatisfying(StockBatchValidationFailed.class,
                        failed -> assertThat(failed.rejected()).hasSize(2));
    }
}
