package me.rongyomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.rongyomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.rongyomes.ecommerce.checkout.saga.message.command.GetCartSnapshotCommand;
import me.rongyomes.ecommerce.checkout.saga.message.command.GetProductSnapshotsCommand;
import me.rongyomes.ecommerce.checkout.saga.message.command.MarkCheckoutCompletedCommand;
import me.rongyomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.rongyomes.ecommerce.checkout.saga.message.event.CartCleared;
import me.rongyomes.ecommerce.checkout.saga.message.event.CartSnapshotProvided;
import me.rongyomes.ecommerce.checkout.saga.message.event.CheckoutRequested;
import me.rongyomes.ecommerce.checkout.saga.message.event.OrderCreated;
import me.rongyomes.ecommerce.checkout.saga.message.event.ProductSnapshotsProvided;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockBatchValidated;
import me.rongyomes.ecommerce.checkout.saga.message.event.StockDeductedForOrder;
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
import static org.mockito.Mockito.mock;
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
    private SagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new SagaOrchestrator(orderBus, cartBus, catalogBus, inventoryBus, objectMapper);
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
    void step1_checkoutRequested_storesSagaStateAndSendsGetCartSnapshot() throws Exception {
        UUID orderId = UUID.randomUUID();

        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "idem-1")));

        assertThat(orchestrator.activeSagas()).containsKey(orderId);
        SagaState state = orchestrator.activeSagas().get(orderId);
        assertThat(state.guestToken).isEqualTo("g1");
        assertThat(state.idempotencyKey).isEqualTo("idem-1");

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(cartBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(GetCartSnapshotCommand.class,
                cmd -> assertThat(cmd.guestToken()).isEqualTo("g1"));
    }

    @Test
    void step2_cartSnapshotProvided_storesItemsAndSendsGetProductSnapshots() throws Exception {
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        CartSnapshotProvided snapshot = new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 2),
                new CartSnapshotProvided.CartItemSnapshot(productB, 3)));

        orchestrator.handle("CartSnapshotProvided", json(snapshot));

        SagaState state = orchestrator.activeSagas().get(orderId);
        assertThat(state.totalItemsToDeduct).isEqualTo(2);
        assertThat(state.cartItems).hasSize(2);

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(catalogBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOfSatisfying(GetProductSnapshotsCommand.class,
                cmd -> assertThat(cmd.productIds()).containsExactly(productA, productB));
    }

    @Test
    void step3_productSnapshotsProvided_storesAndSendsValidateStockBatch() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 4)))));
        ProductSnapshotsProvided snapshots = new ProductSnapshotsProvided(List.of(
                new ProductSnapshotsProvided.ProductSnapshot(productA, "SKU", "Widget", 10.0, true)));

        orchestrator.handle("ProductSnapshotsProvided", json(snapshots));

        SagaState state = orchestrator.activeSagas().get(orderId);
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

        SagaState state = orchestrator.activeSagas().get(orderId);
        assertThat(state.stockValidated).isTrue();
        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(inventoryBus, times(2)).send(sent.capture());
        assertThat(sent.getAllValues().get(1)).isInstanceOfSatisfying(DeductStockForOrderCommand.class, cmd -> {
            assertThat(cmd.orderId()).isEqualTo(orderId);
            assertThat(cmd.items()).hasSize(1);
        });
    }

    @Test
    void step5_partialStockDeductedForOrder_doesNotMarkCompleted() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));
        orchestrator.handle("CartSnapshotProvided", json(new CartSnapshotProvided("g1", List.of(
                new CartSnapshotProvided.CartItemSnapshot(productA, 1),
                new CartSnapshotProvided.CartItemSnapshot(productB, 1)))));

        orchestrator.handle("StockDeductedForOrder", json(new StockDeductedForOrder(orderId, productA, 9)));

        SagaState state = orchestrator.activeSagas().get(orderId);
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
    void step7_cartCleared_sendsSecondMarkCheckoutCompletedAndRemovesSaga_pinsKnownDoubleSendBug() throws Exception {
        // Spec bug §5 #6: MarkCheckoutCompletedCommand is sent both at step 5 (last stock deducted)
        // AND again here. Pinned so a future fix is intentional.
        UUID orderId = UUID.randomUUID();
        orchestrator.handle("CheckoutRequested", json(checkoutRequested(orderId, "g1", "k")));

        orchestrator.handle("CartCleared", json(new CartCleared("g1")));

        ArgumentCaptor<Command<?>> sent = ArgumentCaptor.forClass(Command.class);
        verify(orderBus).send(sent.capture());
        assertThat(sent.getValue()).isInstanceOf(MarkCheckoutCompletedCommand.class);
        assertThat(orchestrator.activeSagas()).doesNotContainKey(orderId);
    }

    @Test
    void fullHappyPath_endsWithSagaRemovedAndMarkCheckoutCompletedSentTwice_pinsBug() throws Exception {
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

        assertThat(orchestrator.activeSagas()).isEmpty();
        verify(orderBus, times(2)).send(any(MarkCheckoutCompletedCommand.class));
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
}
