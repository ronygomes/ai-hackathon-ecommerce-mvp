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
import me.ronygomes.ecommerce.core.application.CommandBus;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SagaOrchestrator {

    private final CommandBus orderBus;
    private final CommandBus cartBus;
    private final CommandBus catalogBus;
    private final CommandBus inventoryBus;
    private final ObjectMapper objectMapper;
    private final SagaStateStore store;

    public SagaOrchestrator(CommandBus orderBus, CommandBus cartBus, CommandBus catalogBus, CommandBus inventoryBus,
                            ObjectMapper objectMapper, SagaStateStore store) {
        this.orderBus = orderBus;
        this.cartBus = cartBus;
        this.catalogBus = catalogBus;
        this.inventoryBus = inventoryBus;
        this.objectMapper = objectMapper;
        this.store = store;
    }

    public SagaStateStore store() {
        return store;
    }

    public void handle(String messageType, String message) throws Exception {
        switch (messageType) {
            case "CheckoutRequested" -> {
                CheckoutRequested cr = objectMapper.readValue(message, CheckoutRequested.class);
                UUID correlationId = UUID.randomUUID();
                SagaState state = new SagaState(cr.orderId(), correlationId, cr.guestToken(), cr.idempotencyKey());
                store.save(state);
                cartBus.send(new GetCartSnapshotCommand(cr.guestToken(), correlationId));
            }
            case "CartSnapshotProvided" -> {
                CartSnapshotProvided csp = objectMapper.readValue(message, CartSnapshotProvided.class);
                SagaState state = store.findByCorrelationId(csp.correlationId()).orElse(null);
                if (state != null) {
                    state.cartItems = csp.items();
                    state.totalItemsToDeduct = csp.items().size();
                    store.save(state);
                    List<UUID> pids = csp.items().stream()
                            .map(CartSnapshotProvided.CartItemSnapshot::productId)
                            .collect(Collectors.toList());
                    catalogBus.send(new GetProductSnapshotsCommand(pids, state.correlationId));
                }
            }
            case "ProductSnapshotsProvided" -> {
                ProductSnapshotsProvided psp = objectMapper.readValue(message, ProductSnapshotsProvided.class);
                SagaState state = store.findByCorrelationId(psp.correlationId()).orElse(null);
                if (state != null) {
                    state.productSnapshots = psp.snapshots();
                    store.save(state);
                    List<ValidateStockBatchCommand.StockItemRequest> items = state.cartItems.stream()
                            .map(i -> new ValidateStockBatchCommand.StockItemRequest(i.productId(), i.qty()))
                            .collect(Collectors.toList());
                    inventoryBus.send(new ValidateStockBatchCommand(items, state.correlationId));
                }
            }
            case "StockBatchValidated" -> {
                StockBatchValidated sbv = objectMapper.readValue(message, StockBatchValidated.class);
                SagaState state = store.findByCorrelationId(sbv.correlationId()).orElse(null);
                if (state != null) {
                    state.stockValidated = true;
                    store.save(state);
                    List<DeductStockForOrderCommand.StockItemRequest> items = state.cartItems.stream()
                            .map(i -> new DeductStockForOrderCommand.StockItemRequest(i.productId(), i.qty()))
                            .collect(Collectors.toList());
                    inventoryBus.send(new DeductStockForOrderCommand(state.orderId, items));
                }
            }
            case "StockDeductedForOrder" -> {
                StockDeductedForOrder sdf = objectMapper.readValue(message, StockDeductedForOrder.class);
                SagaState state = store.findByOrderId(sdf.orderId()).orElse(null);
                if (state != null) {
                    state.deductedItemsCount++;
                    store.save(state);
                    if (state.deductedItemsCount >= state.totalItemsToDeduct) {
                        orderBus.send(new MarkCheckoutCompletedCommand(state.orderId));
                    }
                }
            }
            case "OrderCreated" -> {
                OrderCreated oc = objectMapper.readValue(message, OrderCreated.class);
                SagaState state = store.findByOrderId(UUID.fromString(oc.orderId())).orElse(null);
                if (state != null) {
                    cartBus.send(new ClearCartCommand(state.guestToken, state.correlationId));
                }
            }
            case "CartCleared" -> {
                CartCleared cc = objectMapper.readValue(message, CartCleared.class);
                SagaState state = store.findByCorrelationId(cc.correlationId()).orElse(null);
                if (state != null) {
                    store.remove(state.orderId);
                }
            }
            case "StockBatchValidationFailed" -> {
                StockBatchValidationFailed evt = objectMapper.readValue(message, StockBatchValidationFailed.class);
                SagaState state = store.findByCorrelationId(evt.correlationId()).orElse(null);
                if (state != null) {
                    System.err.println("Saga ABORTED for order " + state.orderId
                            + ": stock validation failed for " + evt.rejected().size() + " item(s)");
                    store.remove(state.orderId);
                }
            }
            case "StockDeductionFailed" -> {
                StockDeductionFailed evt = objectMapper.readValue(message, StockDeductionFailed.class);
                SagaState state = store.findByOrderId(evt.orderId()).orElse(null);
                if (state != null) {
                    System.err.println("Saga ABORTED for order " + state.orderId
                            + ": deduction failed for product " + evt.productId() + " (" + evt.reason() + ")");
                    store.remove(state.orderId);
                }
            }
            default -> System.err.println("No handler for message type: " + messageType);
        }
    }
}
