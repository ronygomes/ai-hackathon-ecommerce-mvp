package me.ronygomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.ronygomes.ecommerce.checkout.saga.message.command.*;
import me.ronygomes.ecommerce.checkout.saga.message.event.*;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.ProcessedCommandStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SagaOrchestratorTest {

    private final CommandBus orderBus = mock(CommandBus.class);
    private final CommandBus cartBus = mock(CommandBus.class);
    private final CommandBus catalogBus = mock(CommandBus.class);
    private final CommandBus inventoryBus = mock(CommandBus.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InMemorySagaStateStore store = new InMemorySagaStateStore();
    private SagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new SagaOrchestrator(orderBus, cartBus, catalogBus, inventoryBus, objectMapper, store);
        when(orderBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(cartBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(catalogBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(inventoryBus.send(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private String json(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    private CheckoutRequested checkoutRequested(UUID orderId, String guestToken, String idempotencyKey) {
        return new CheckoutRequested(orderId, guestToken, guestToken, "Jane", "+1", "j@e",
                "L", "C", "x", "US", idempotencyKey);
    }

    /**
     * Pull the correlationId the orchestrator generated when it processed CheckoutRequested.
     */
    private UUID correlationOf(UUID orderId) {
        return store.findByOrderId(orderId).orElseThrow().correlationId;
    }

    @Test
    void step1_checkoutRequested_persistsSagaStateAndSendsGetCartSnapshotWithCorrelationId() throws Exception {
        UUID orderId = UUID.randomUUID();

        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "idem-1")));

        SagaState state = store.findByOrderId(orderId).orElseThrow();
        assertThat(state.guestToken).isEqualTo("g1");
        assertThat(state.idempotencyKey).isEqualTo("idem-1");
        assertThat(state.correlationId).isNotNull();

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(cartBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(GetCartSnapshotCommand.class, cmd -> {
            assertThat(cmd.guestToken()).isEqualTo("g1");
            assertThat(cmd.correlationId()).isEqualTo(state.correlationId);
        });
    }

    @Test
    void step1_concurrentCheckoutRequests_generateDistinctCorrelationIds() throws Exception {
        UUID orderA = UUID.randomUUID();
        UUID orderB = UUID.randomUUID();

        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderA, "ga", "ka")));
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderB, "gb", "kb")));

        UUID corrA = correlationOf(orderA);
        UUID corrB = correlationOf(orderB);
        assertThat(corrA).isNotEqualTo(corrB);
    }

    @Test
    void step2_cartSnapshotProvided_lookedUpByCorrelationId_sendsGetProductSnapshots() throws Exception {
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        CartSnapshotProvided snapshot = new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 2),
                new CartSnapshotProvided.CartItemSnapshot(productB, 3)),
                correlationId, "test-cause");

        orchestrator.handle("CartSnapshotProvided", json(snapshot));

        SagaState state = store.findByOrderId(orderId).orElseThrow();
        assertThat(state.totalItemsToDeduct).isEqualTo(2);
        assertThat(state.cartItems).hasSize(2);

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(catalogBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(GetProductSnapshotsCommand.class, cmd -> {
            assertThat(cmd.productIds()).containsExactly(productA, productB);
            assertThat(cmd.correlationId()).isEqualTo(correlationId);
        });
    }

    @Test
    void step3_productSnapshotsProvided_lookedUpByCorrelationId_sendsValidateStockBatch() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 4)), correlationId, "test-cause")));
        ProductSnapshotsProvided snapshots = new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 10.0, true)),
                correlationId, "test-cause");

        orchestrator.handle("ProductSnapshotsProvided", json(snapshots));

        SagaState state = store.findByOrderId(orderId).orElseThrow();
        assertThat(state.productSnapshots).hasSize(1);
        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(inventoryBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(ValidateStockBatchCommand.class, cmd -> {
            assertThat(cmd.items()).hasSize(1);
            assertThat(cmd.correlationId()).isEqualTo(correlationId);
        });
    }

    @Test
    void step4_stockBatchValidated_lookedUpByCorrelationId_sendsDeductStockForOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 2)), correlationId, "test-cause")));
        orchestrator.handle("ProductSnapshotsProvided", json(new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 1.0, true)),
                correlationId, "test-cause")));

        orchestrator.handle("StockBatchValidated", json(new StockBatchValidated(correlationId, "test-cause")));

        SagaState state = store.findByOrderId(orderId).orElseThrow();
        assertThat(state.stockValidated).isTrue();
        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(inventoryBus, times(2)).send(sent.capture());
        assertThat(sent.getAllValues().get(1)).isInstanceOfSatisfying(DeductStockForOrderCommand.class, cmd -> {
            assertThat(cmd.orderId()).isEqualTo(orderId);
            assertThat(cmd.items()).hasSize(1);
        });
    }

    @Test
    void step5_partialStockDeductedForOrder_persistsCountWithoutFinalizing() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1),
                new CartSnapshotProvided.CartItemSnapshot(productB, 1)), correlationId, "test-cause")));

        orchestrator.handle("StockDeductedForOrder", json(new StockDeductedForOrder(orderId, productA, 9)));

        SagaState state = store.findByOrderId(orderId).orElseThrow();
        assertThat(state.deductedItemsCount).isEqualTo(1);
        verifyNoInteractions(orderBus);
    }

    @Test
    void step5_lastStockDeductedForOrder_sendsMarkCheckoutCompletedCommand() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1)), correlationId, "test-cause")));

        orchestrator.handle("StockDeductedForOrder", json(new StockDeductedForOrder(orderId, productA, 9)));

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(orderBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(MarkCheckoutCompletedCommand.class,
                cmd -> assertThat(cmd.orderId()).isEqualTo(orderId));
    }

    @Test
    void step6_orderCreated_sendsClearCartCommandWithSagaCorrelationId() throws Exception {
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);

        orchestrator.handle("OrderCreated", json(new OrderCreated(orderId.toString(), "g1", "j@e")));

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(cartBus, times(2)).send(sent.capture());
        assertThat(sent.getAllValues().get(1)).isInstanceOfSatisfying(ClearCartCommand.class, cmd -> {
            assertThat(cmd.guestToken()).isEqualTo("g1");
            assertThat(cmd.correlationId()).isEqualTo(correlationId);
        });
    }

    @Test
    void step7_cartCleared_lookedUpByCorrelationId_removesSagaWithoutResendingMarkCheckoutCompleted() throws Exception {
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);

        orchestrator.handle("CartCleared", json(new CartCleared("g1", correlationId, "test-cause")));

        assertThat(store.findByOrderId(orderId)).isEmpty();
        verifyNoInteractions(orderBus);
    }

    @Test
    void step7_cartCleared_withForeignCorrelationId_isNoOp() throws Exception {
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));

        // User-initiated cart clear with a fresh correlationId — saga should ignore it.
        orchestrator.handle("CartCleared", json(new CartCleared("g1", UUID.randomUUID(), "test-cause")));

        assertThat(store.findByOrderId(orderId)).isPresent();
    }

    @Test
    void fullHappyPath_endsWithSagaRemovedAndMarkCheckoutCompletedSentOnce() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1)), correlationId, "test-cause")));
        orchestrator.handle("ProductSnapshotsProvided", json(new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 1.0, true)),
                correlationId, "test-cause")));
        orchestrator.handle("StockBatchValidated", json(new StockBatchValidated(correlationId, "test-cause")));
        orchestrator.handle("StockDeductedForOrder", json(new StockDeductedForOrder(orderId, productA, 0)));
        orchestrator.handle("OrderCreated", json(new OrderCreated(orderId.toString(), "g1", "j@e")));
        orchestrator.handle("CartCleared", json(new CartCleared("g1", correlationId, "test-cause")));

        assertThat(store.findAll()).isEmpty();
        verify(orderBus, times(1)).send(any(MarkCheckoutCompletedCommand.class));
    }

    @Test
    void unknownMessageType_isNoOp() throws Exception {
        orchestrator.handle("WhatIsThis", "{}");

        verifyNoInteractions(orderBus);
        verifyNoInteractions(cartBus);
        verifyNoInteractions(catalogBus);
        verifyNoInteractions(inventoryBus);
    }

    @Test
    void eventForUnknownSaga_isNoOp() throws Exception {
        orchestrator.handle("StockDeductedForOrder",
                json(new StockDeductedForOrder(UUID.randomUUID(), UUID.randomUUID(), 0)));

        verifyNoInteractions(orderBus);
    }

    @Test
    void store_isAccessibleViaAccessor() {
        assertThat(orchestrator.store()).isSameAs(store);
    }

    @Test
    void stockBatchValidationFailed_lookedUpByCorrelationId_sendsCancelOrderCommandAndRemovesSaga() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1)), correlationId, "test-cause")));
        orchestrator.handle("ProductSnapshotsProvided", json(new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 1.0, true)),
                correlationId, "test-cause")));

        orchestrator.handle("StockBatchValidationFailed", json(new StockBatchValidationFailed(List.of(
                new StockBatchValidationFailed.RejectedItem(productA, 1, 0, "Insufficient stock")),
                correlationId, "test-cause")));

        assertThat(store.findByOrderId(orderId)).isEmpty();
        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(orderBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(CancelOrderCommand.class, cmd -> {
            assertThat(cmd.orderId()).isEqualTo(orderId);
            assertThat(cmd.reason()).contains("stock validation failed");
        });
    }

    @Test
    void stockBatchValidationFailed_withNoMatchingSaga_isNoOp() throws Exception {
        orchestrator.handle("StockBatchValidationFailed", json(new StockBatchValidationFailed(List.of(
                new StockBatchValidationFailed.RejectedItem(UUID.randomUUID(), 1, 0, "x")),
                UUID.randomUUID(), "test-cause")));

        assertThat(store.findAll()).isEmpty();
        verifyNoInteractions(orderBus);
    }

    @Test
    void stockDeductionFailed_sendsCancelOrderCommandAndRemovesSagaByOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 5)), correlationId, "test-cause")));

        orchestrator.handle("StockDeductionFailed",
                json(new StockDeductionFailed(orderId, productA, 5, 1, "Insufficient stock")));

        assertThat(store.findByOrderId(orderId)).isEmpty();
        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(orderBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(CancelOrderCommand.class, cmd -> {
            assertThat(cmd.orderId()).isEqualTo(orderId);
            assertThat(cmd.reason()).contains("stock deduction failed");
            assertThat(cmd.reason()).contains(productA.toString());
        });
    }

    @Test
    void stockDeductionFailed_forUnknownSaga_isNoOp() throws Exception {
        orchestrator.handle("StockDeductionFailed",
                json(new StockDeductionFailed(UUID.randomUUID(), UUID.randomUUID(), 1, 0, "x")));

        assertThat(store.findAll()).isEmpty();
        verifyNoInteractions(orderBus);
    }

    @Test
    void everyMutatingStepCallsStoreSave() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        SagaStateStore spy = mock(SagaStateStore.class);
        when(spy.findByOrderId(any())).thenAnswer(inv -> store.findByOrderId(inv.getArgument(0)));
        when(spy.findByCorrelationId(any())).thenAnswer(inv -> store.findByCorrelationId(inv.getArgument(0)));
        when(spy.findAll()).thenAnswer(inv -> store.findAll());
        doAnswer(inv -> {
            store.save(inv.getArgument(0));
            return null;
        }).when(spy).save(any());
        doAnswer(inv -> {
            store.remove(inv.getArgument(0));
            return null;
        }).when(spy).remove(any());

        SagaOrchestrator persistentOrchestrator = new SagaOrchestrator(
                orderBus, cartBus, catalogBus, inventoryBus, objectMapper, spy);

        persistentOrchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        persistentOrchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1)), correlationId, "test-cause")));
        persistentOrchestrator.handle("ProductSnapshotsProvided", json(new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "W", 1.0, true)), correlationId, "test-cause")));
        persistentOrchestrator.handle("StockBatchValidated", json(new StockBatchValidated(correlationId, "test-cause")));
        persistentOrchestrator.handle("StockDeductedForOrder", json(new StockDeductedForOrder(orderId, productA, 0)));

        verify(spy, times(5)).save(any());
        verify(spy, never()).remove(any());
    }

    /**
     * In-memory ProcessedCommandStore stand-in so tests can verify duplicate-event
     * suppression without spinning up Mongo.
     */
    private static final class InMemoryProcessedEventStore implements ProcessedCommandStore {
        private final Set<String> seen = new HashSet<>();

        @Override
        public boolean wasProcessed(String id) {
            return seen.contains(id);
        }

        @Override
        public void markProcessed(String id, String metadata) {
            seen.add(id);
        }
    }

    @Test
    void withIdempotencyStore_duplicateCheckoutRequested_isHandledOnceAndDoesNotResendCommand()
            throws Exception {
        InMemoryProcessedEventStore eventStore = new InMemoryProcessedEventStore();
        SagaOrchestrator idempotent = new SagaOrchestrator(
                orderBus, cartBus, catalogBus, inventoryBus, objectMapper, store, eventStore);

        UUID orderId = UUID.randomUUID();
        String body = json(checkoutRequested(orderId, "g1", "k1"));

        idempotent.handle("CheckoutRequested", body);
        idempotent.handle("CheckoutRequested", body); // redelivery — same eventId

        verify(cartBus, times(1)).send(any(GetCartSnapshotCommand.class));
        assertThat(store.findByOrderId(orderId)).isPresent();
    }

    @Test
    void withIdempotencyStore_duplicateStockDeductedForOrder_doesNotDoubleCountDeductions()
            throws Exception {
        InMemoryProcessedEventStore eventStore = new InMemoryProcessedEventStore();
        SagaOrchestrator idempotent = new SagaOrchestrator(
                orderBus, cartBus, catalogBus, inventoryBus, objectMapper, store, eventStore);

        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        idempotent.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID correlationId = correlationOf(orderId);
        idempotent.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1),
                new CartSnapshotProvided.CartItemSnapshot(productB, 1)), correlationId, "test-cause")));

        String deductBody = json(new StockDeductedForOrder(orderId, productA, 0));
        idempotent.handle("StockDeductedForOrder", deductBody);
        idempotent.handle("StockDeductedForOrder", deductBody); // redelivery

        SagaState state = store.findByOrderId(orderId).orElseThrow();
        assertThat(state.deductedItemsCount).isEqualTo(1); // not 2
        verifyNoInteractions(orderBus); // still 1 short of completion
    }
}
