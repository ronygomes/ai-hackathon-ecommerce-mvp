package com.ecommerce.checkout.saga;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.infrastructure.RabbitMQCommandBus;
import com.ecommerce.ordering.application.*;
import com.ecommerce.ordering.domain.*;
import com.ecommerce.cart.application.GetCartSnapshotCommand;
import com.ecommerce.cart.application.ClearCartCommand;
import com.ecommerce.cart.domain.CartSnapshotProvided;
import com.ecommerce.productcatalog.application.GetProductSnapshotsCommand;
import com.ecommerce.productcatalog.domain.ProductSnapshotsProvided;
import com.ecommerce.inventory.application.ValidateStockBatchCommand;
import com.ecommerce.inventory.application.DeductStockForOrderCommand;
import com.ecommerce.inventory.domain.StockBatchValidated;
import com.ecommerce.inventory.domain.StockDeductedForOrder;
import tools.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CheckoutSagaProcess {
    private static final Map<UUID, SagaState> activeSagas = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        ICommandBus orderBus = new RabbitMQCommandBus("ordering_commands", "localhost");
        ICommandBus cartBus = new RabbitMQCommandBus("cart_commands", "localhost");
        ICommandBus catalogBus = new RabbitMQCommandBus("product_catalog_commands", "localhost");
        ICommandBus inventoryBus = new RabbitMQCommandBus("inventory_commands", "localhost");

        ObjectMapper objectMapper = new ObjectMapper();

        String queueName = "checkout_saga_coordinator";
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, "ordering_events", "");
        channel.queueBind(queueName, "cart_events", "");
        channel.queueBind(queueName, "product_catalog_events", "");
        channel.queueBind(queueName, "inventory_events", "");

        System.out.println("CheckoutSaga Coordinator waiting for events...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = (headers != null && headers.get("X-Message-Type") != null)
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("CheckoutRequested".equals(messageType)) {
                    CheckoutRequested event = objectMapper.readValue(message, CheckoutRequested.class);
                    activeSagas.put(event.orderId(),
                            new SagaState(event.orderId(), event.guestToken(), event.idempotencyKey()));
                    cartBus.send(new GetCartSnapshotCommand(event.guestToken()));
                } else if ("CartSnapshotProvided".equals(messageType)) {
                    CartSnapshotProvided event = objectMapper.readValue(message, CartSnapshotProvided.class);
                    SagaState state = activeSagas.values().stream().filter(s -> s.guestToken.equals(event.guestToken()))
                            .findFirst().orElse(null);
                    if (state != null) {
                        state.cartItems = event.items();
                        List<UUID> pids = event.items().stream()
                                .map(CartSnapshotProvided.CartItemSnapshot::productId)
                                .collect(Collectors.toList());
                        catalogBus.send(new GetProductSnapshotsCommand(pids));
                    }
                } else if ("ProductSnapshotsProvided".equals(messageType)) {
                    ProductSnapshotsProvided event = objectMapper.readValue(message, ProductSnapshotsProvided.class);
                    SagaState state = activeSagas.values().stream()
                            .filter(s -> s.cartItems != null && s.productSnapshots == null).findFirst().orElse(null);
                    if (state != null) {
                        state.productSnapshots = event.snapshots();
                        List<com.ecommerce.inventory.application.ValidateStockBatchCommand.StockItemRequest> items = state.cartItems
                                .stream()
                                .map(i -> new com.ecommerce.inventory.application.ValidateStockBatchCommand.StockItemRequest(
                                        i.productId(), i.qty()))
                                .collect(Collectors.toList());
                        inventoryBus.send(new ValidateStockBatchCommand(items));
                    }
                } else if ("StockBatchValidated".equals(messageType)) {
                    SagaState state = activeSagas.values().stream()
                            .filter(s -> s.productSnapshots != null && s.cartItems != null && !s.stockValidated)
                            .findFirst().orElse(null);
                    if (state != null) {
                        state.stockValidated = true;
                        List<com.ecommerce.inventory.application.DeductStockForOrderCommand.StockItemRequest> items = state.cartItems
                                .stream()
                                .map(i -> new com.ecommerce.inventory.application.DeductStockForOrderCommand.StockItemRequest(
                                        i.productId(), i.qty()))
                                .collect(Collectors.toList());
                        inventoryBus.send(new DeductStockForOrderCommand(state.orderId, items));
                    }
                } else if ("StockDeductedForOrder".equals(messageType)) {
                    StockDeductedForOrder event = objectMapper.readValue(message, StockDeductedForOrder.class);
                    SagaState state = activeSagas.get(event.orderId());
                    if (state != null) {
                        List<com.ecommerce.ordering.application.CreateOrderCommand.OrderItemRequest> items = state.cartItems
                                .stream().map(ci -> {
                                    com.ecommerce.productcatalog.domain.ProductSnapshotsProvided.ProductSnapshot ps = state.productSnapshots
                                            .stream()
                                            .filter(s -> s.productId().equals(ci.productId())).findFirst().get();
                                    return new com.ecommerce.ordering.application.CreateOrderCommand.OrderItemRequest(
                                            ci.productId(), ps.sku(), ps.name(), ps.price(), ci.qty());
                                }).collect(Collectors.toList());

                        orderBus.send(new CreateOrderCommand(state.orderId, state.guestToken, null, null,
                                state.idempotencyKey, items));
                    }
                } else if ("OrderCreated".equals(messageType)) {
                    OrderCreated event = objectMapper.readValue(message, OrderCreated.class);
                    SagaState state = activeSagas.get(event.orderId());
                    if (state != null) {
                        cartBus.send(new ClearCartCommand(state.guestToken));
                    }
                } else if ("CartCleared".equals(messageType)) {
                    SagaState state = activeSagas.values().stream().filter(s -> s.stockValidated).findFirst()
                            .orElse(null);
                    if (state != null) {
                        orderBus.send(new MarkCheckoutCompletedCommand(state.orderId));
                        activeSagas.remove(state.orderId);
                        System.out.println("Saga COMPLETED for order: " + state.orderId);
                    }
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }

    private static class SagaState {
        UUID orderId;
        String guestToken;
        String idempotencyKey;
        boolean stockValidated = false;
        List<CartSnapshotProvided.CartItemSnapshot> cartItems;
        List<ProductSnapshotsProvided.ProductSnapshot> productSnapshots;

        SagaState(UUID orderId, String guestToken, String idempotencyKey) {
            this.orderId = orderId;
            this.guestToken = guestToken;
            this.idempotencyKey = idempotencyKey;
        }
    }
}
