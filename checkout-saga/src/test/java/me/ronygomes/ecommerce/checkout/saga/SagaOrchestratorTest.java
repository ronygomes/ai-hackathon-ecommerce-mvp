package me.ronygomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.ronygomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.ronygomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.ronygomes.ecommerce.checkout.saga.message.command.GetCartSnapshotCommand;
import me.ronygomes.ecommerce.checkout.saga.message.command.GetProductSnapshotsCommand;
import me.ronygomes.ecommerce.checkout.saga.message.command.MarkCheckoutCompletedCommand;
import me.ronygomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.CartCleared;
import me.ronygomes.ecommerce.checkout.saga.message.event.CartSnapshotProvided;
import me.ronygomes.ecommerce.checkout.saga.message.event.CheckoutRequested;
import me.ronygomes.ecommerce.checkout.saga.message.event.OrderCreated;
import me.ronygomes.ecommerce.checkout.saga.message.event.ProductSnapshotsProvided;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockBatchValidated;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockBatchValidationFailed;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockDeductedForOrder;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockDeductionFailed;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @Test
    void step1_checkoutRequested_persistsSagaStateAndSendsGetCartSnapshot() throws Exception {
        UUID orderId = UUID.randomUUID();

        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "idem-1")));

        assertThat(store.findByOrderId(orderId)).hasValueSatisfying(state -> {
            assertThat(state.guestToken).isEqualTo("g1");
            assertThat(state.idempotencyKey).isEqualTo("idem-1");
        });

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(cartBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(GetCartSnapshotCommand.class,
                cmd -> assertThat(cmd.guestToken()).isEqualTo("g1"));
    }

    @Test
    void step2_cartSnapshotProvided_persistsItemsAndSendsGetProductSnapshots() throws Exception {
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        CartSnapshotProvided snapshot = new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 2),
                new CartSnapshotProvided.CartItemSnapshot(productB, 3)));

        orchestrator.handle("CartSnapshotProvided", json(snapshot));

        SagaState state = store.findByOrderId(orderId).orElseThrow();
        assertThat(state.totalItemsToDeduct).isEqualTo(2);
        assertThat(state.cartItems).hasSize(2);

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(catalogBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(GetProductSnapshotsCommand.class,
                cmd -> assertThat(cmd.productIds()).containsExactly(productA, productB));
    }

    @Test
    void step3_productSnapshotsProvided_persistsAndSendsValidateStockBatch() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 4)))));
        ProductSnapshotsProvided snapshots = new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 10.0, true)));

        orchestrator.handle("ProductSnapshotsProvided", json(snapshots));

        SagaState state = store.findByOrderId(orderId).orElseThrow();
        assertThat(state.productSnapshots).hasSize(1);
        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(inventoryBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(ValidateStockBatchCommand.class,
                cmd -> assertThat(cmd.items()).hasSize(1));
    }

    @Test
    void step4_stockBatchValidated_marksValidatedAndSendsDeductStockForOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 2)))));
        orchestrator.handle("ProductSnapshotsProvided", json(new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 1.0, true)))));

        orchestrator.handle("StockBatchValidated", json(new StockBatchValidated()));

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
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1),
                new CartSnapshotProvided.CartItemSnapshot(productB, 1)))));

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
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1)))));

        orchestrator.handle("StockDeductedForOrder", json(new StockDeductedForOrder(orderId, productA, 9)));

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(orderBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(MarkCheckoutCompletedCommand.class,
                cmd -> assertThat(cmd.orderId()).isEqualTo(orderId));
    }

    @Test
    void step6_orderCreated_sendsClearCartCommand() throws Exception {
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));

        orchestrator.handle("OrderCreated", json(new OrderCreated(orderId.toString(), "g1", "j@e")));

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(cartBus, times(2)).send(sent.capture());
        assertThat(sent.getAllValues().get(1)).isInstanceOfSatisfying(ClearCartCommand.class,
                cmd -> assertThat(cmd.guestToken()).isEqualTo("g1"));
    }

    @Test
    void step7_cartCleared_removesSagaWithoutResendingMarkCheckoutCompleted() throws Exception {
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));

        orchestrator.handle("CartCleared", json(new CartCleared("g1")));

        assertThat(store.findByOrderId(orderId)).isEmpty();
        verifyNoInteractions(orderBus);
    }

    @Test
    void fullHappyPath_endsWithSagaRemovedAndMarkCheckoutCompletedSentOnce() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1)))));
        orchestrator.handle("ProductSnapshotsProvided", json(new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 1.0, true)))));
        orchestrator.handle("StockBatchValidated", json(new StockBatchValidated()));
        orchestrator.handle("StockDeductedForOrder", json(new StockDeductedForOrder(orderId, productA, 0)));
        orchestrator.handle("OrderCreated", json(new OrderCreated(orderId.toString(), "g1", "j@e")));
        orchestrator.handle("CartCleared", json(new CartCleared("g1")));

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
    void stockBatchValidationFailed_removesSagaWaitingForValidation() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1)))));
        orchestrator.handle("ProductSnapshotsProvided", json(new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 1.0, true)))));

        orchestrator.handle("StockBatchValidationFailed", json(new StockBatchValidationFailed(List.of(
                new StockBatchValidationFailed.RejectedItem(productA, 1, 0, "Insufficient stock")))));

        assertThat(store.findByOrderId(orderId)).isEmpty();
        verifyNoInteractions(orderBus);
    }

    @Test
    void stockBatchValidationFailed_withNoMatchingSaga_isNoOp() throws Exception {
        orchestrator.handle("StockBatchValidationFailed", json(new StockBatchValidationFailed(List.of(
                new StockBatchValidationFailed.RejectedItem(UUID.randomUUID(), 1, 0, "x")))));

        assertThat(store.findAll()).isEmpty();
        verifyNoInteractions(orderBus);
    }

    @Test
    void stockDeductionFailed_removesSagaByOrderId() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 5)))));

        orchestrator.handle("StockDeductionFailed",
                json(new StockDeductionFailed(orderId, productA, 5, 1, "Insufficient stock")));

        assertThat(store.findByOrderId(orderId)).isEmpty();
        verifyNoInteractions(orderBus);
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
        persistentOrchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1)))));
        persistentOrchestrator.handle("ProductSnapshotsProvided", json(new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "W", 1.0, true)))));
        persistentOrchestrator.handle("StockBatchValidated", json(new StockBatchValidated()));
        persistentOrchestrator.handle("StockDeductedForOrder", json(new StockDeductedForOrder(orderId, productA, 0)));

        verify(spy, times(5)).save(any());
        verify(spy, never()).remove(any());
    }
}
