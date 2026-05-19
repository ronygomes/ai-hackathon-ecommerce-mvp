package me.ronygomes.ecommerce.inventory.application;

import me.ronygomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockDeductedForOrder;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockDeductionFailed;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeductStockForOrderHandlerTest {

    @SuppressWarnings("unchecked")
    private final Repository<InventoryItem, ProductId> repository = mock(Repository.class);
    private final OutboxStore outboxStore = mock(OutboxStore.class);
    private DeductStockForOrderHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DeductStockForOrderHandler(repository, outboxStore);
        when(repository.save(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private static InventoryItem stocked(int qty) {
        return InventoryItem.create(ProductId.generate(), new Quantity(qty));
    }

    private record AppendCall(String aggregateId, List<DomainEvent> events) {
    }

    private List<AppendCall> captureAppendCalls() {
        List<AppendCall> calls = new ArrayList<>();
        doAnswer(inv -> {
            calls.add(new AppendCall(inv.getArgument(0), new ArrayList<>(inv.getArgument(1))));
            return null;
        }).when(outboxStore).append(any(), any());
        return calls;
    }

    @Test
    void handle_deductsEveryItemAndAppendsOneEventPerItemKeyedByProductId() throws Exception {
        InventoryItem a = stocked(20);
        InventoryItem b = stocked(15);
        a.clearUncommittedEvents();
        b.clearUncommittedEvents();
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(a)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(b)));
        List<AppendCall> appended = captureAppendCalls();

        handler.handle(new DeductStockForOrderCommand(UUID.randomUUID(), List.of(
                new DeductStockForOrderCommand.StockItemRequest(UUID.randomUUID(), 5),
                new DeductStockForOrderCommand.StockItemRequest(UUID.randomUUID(), 7)))).get();

        assertThat(a.getQuantity().value()).isEqualTo(15);
        assertThat(b.getQuantity().value()).isEqualTo(8);
        verify(repository, times(2)).save(any());
        assertThat(appended).hasSize(2);
        assertThat(appended.get(0).aggregateId()).isEqualTo(a.getId().toString());
        assertThat(appended.get(0).events()).singleElement().isInstanceOf(StockDeductedForOrder.class);
        assertThat(appended.get(1).aggregateId()).isEqualTo(b.getId().toString());
        assertThat(appended.get(1).events()).singleElement().isInstanceOf(StockDeductedForOrder.class);
    }

    @Test
    void handle_itemNotFound_appendsStockDeductionFailedKeyedByOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID missingProductId = UUID.randomUUID();
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        List<AppendCall> appended = captureAppendCalls();

        handler.handle(new DeductStockForOrderCommand(orderId, List.of(
                new DeductStockForOrderCommand.StockItemRequest(missingProductId, 3)))).get();

        assertThat(appended).singleElement().satisfies(call -> {
            assertThat(call.aggregateId()).isEqualTo(orderId.toString());
            assertThat(call.events()).singleElement()
                    .isInstanceOfSatisfying(StockDeductionFailed.class, failed -> {
                        assertThat(failed.orderId()).isEqualTo(orderId);
                        assertThat(failed.productId()).isEqualTo(missingProductId);
                        assertThat(failed.requestedQty()).isEqualTo(3);
                        assertThat(failed.availableQty()).isZero();
                        assertThat(failed.reason()).isEqualTo("Stock item not found");
                    });
        });
    }

    @Test
    void handle_insufficientStock_appendsStockDeductionFailedAndDoesNotThrow() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryItem item = stocked(2);
        when(repository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(item)));
        List<AppendCall> appended = captureAppendCalls();

        handler.handle(new DeductStockForOrderCommand(orderId, List.of(
                new DeductStockForOrderCommand.StockItemRequest(productId, 5)))).get();

        assertThat(item.getQuantity().value()).isEqualTo(2);
        assertThat(appended).singleElement().satisfies(call ->
                assertThat(call.events()).singleElement()
                        .isInstanceOfSatisfying(StockDeductionFailed.class, failed -> {
                            assertThat(failed.requestedQty()).isEqualTo(5);
                            assertThat(failed.availableQty()).isEqualTo(2);
                            assertThat(failed.reason()).isEqualTo("Insufficient stock");
                        }));
    }

    @Test
    void handle_mixedSuccessAndFailure_eachItemEmitsItsOwnEvent() throws Exception {
        UUID orderId = UUID.randomUUID();
        InventoryItem ok = stocked(10);
        ok.clearUncommittedEvents();
        when(repository.getById(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(ok)))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        List<AppendCall> appended = captureAppendCalls();

        handler.handle(new DeductStockForOrderCommand(orderId, List.of(
                new DeductStockForOrderCommand.StockItemRequest(UUID.randomUUID(), 3),
                new DeductStockForOrderCommand.StockItemRequest(UUID.randomUUID(), 1)))).get();

        assertThat(appended).hasSize(2);
        assertThat(appended.get(0).events()).singleElement().isInstanceOf(StockDeductedForOrder.class);
        assertThat(appended.get(1).events()).singleElement().isInstanceOf(StockDeductionFailed.class);
    }
}
