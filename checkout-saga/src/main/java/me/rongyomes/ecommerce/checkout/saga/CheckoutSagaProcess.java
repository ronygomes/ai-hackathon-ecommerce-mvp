package me.rongyomes.ecommerce.checkout.saga;

import com.rabbitmq.client.*;
import me.rongyomes.ecommerce.checkout.saga.message.command.*;
import me.rongyomes.ecommerce.checkout.saga.message.event.*;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CheckoutSagaProcess {
    private static final Map<UUID, SagaState> activeSagas = new ConcurrentHashMap<>();

    static void main() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        CommandBus orderBus = new RabbitMQCommandBus("ordering_commands", "localhost");
        CommandBus cartBus = new RabbitMQCommandBus("cart_commands", "localhost");
        CommandBus catalogBus = new RabbitMQCommandBus("product_catalog_commands", "localhost");
        CommandBus inventoryBus = new RabbitMQCommandBus("inventory_commands", "localhost");

        ObjectMapper objectMapper = new ObjectMapper();

        String queueName = "checkout_saga_coordinator";
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, "ordering_events", "");
        channel.queueBind(queueName, "cart_events", "");
        channel.queueBind(queueName, "product_catalog_events", "");
        channel.queueBind(queueName, "inventory_events", "");

        System.out.println("CheckoutSaga Coordinator waiting for events (Switch Case Pattern)...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = (headers != null && headers.get("X-Message-Type") != null)
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                switch (messageType) {
                    case "CheckoutRequested":
                        CheckoutRequested cr = objectMapper.readValue(message, CheckoutRequested.class);
                        SagaState stateCR = new SagaState(cr.orderId(), cr.guestToken(), cr.idempotencyKey());
                        activeSagas.put(cr.orderId(), stateCR);
                        cartBus.send(new GetCartSnapshotCommand(cr.guestToken()));
                        break;

                    case "CartSnapshotProvided":
                        CartSnapshotProvided csp = objectMapper.readValue(message, CartSnapshotProvided.class);
                        SagaState stateCSP = activeSagas.values().stream()
                                .filter(s -> s.guestToken.equals(csp.guestToken()))
                                .findFirst().orElse(null);
                        if (stateCSP != null) {
                            stateCSP.cartItems = csp.items();
                            stateCSP.totalItemsToDeduct = csp.items().size();
                            java.util.List<UUID> pids = csp.items().stream()
                                    .map(CartSnapshotProvided.CartItemSnapshot::productId)
                                    .collect(Collectors.toList());
                            catalogBus.send(new GetProductSnapshotsCommand(pids));
                        }
                        break;

                    case "ProductSnapshotsProvided":
                        ProductSnapshotsProvided psp = objectMapper.readValue(message, ProductSnapshotsProvided.class);
                        SagaState statePSP = activeSagas.values().stream()
                                .filter(s -> s.cartItems != null && s.productSnapshots == null)
                                .findFirst().orElse(null);
                        if (statePSP != null) {
                            statePSP.productSnapshots = psp.snapshots();
                            java.util.List<ValidateStockBatchCommand.StockItemRequest> items = statePSP.cartItems
                                    .stream()
                                    .map(i -> new ValidateStockBatchCommand.StockItemRequest(i.productId(), i.qty()))
                                    .collect(Collectors.toList());
                            inventoryBus.send(new ValidateStockBatchCommand(items));
                        }
                        break;

                    case "StockBatchValidated":
                        objectMapper.readValue(message, StockBatchValidated.class);
                        SagaState stateSBV = activeSagas.values().stream()
                                .filter(s -> s.productSnapshots != null && s.cartItems != null && !s.stockValidated)
                                .findFirst().orElse(null);
                        if (stateSBV != null) {
                            stateSBV.stockValidated = true;
                            java.util.List<DeductStockForOrderCommand.StockItemRequest> items = stateSBV.cartItems
                                    .stream()
                                    .map(i -> new DeductStockForOrderCommand.StockItemRequest(i.productId(), i.qty()))
                                    .collect(Collectors.toList());
                            inventoryBus.send(new DeductStockForOrderCommand(stateSBV.orderId, items));
                        }
                        break;

                    case "StockDeductedForOrder":
                        StockDeductedForOrder sdf = objectMapper.readValue(message, StockDeductedForOrder.class);
                        SagaState stateSDF = activeSagas.get(sdf.orderId());
                        if (stateSDF != null) {
                            stateSDF.deductedItemsCount++;
                            if (stateSDF.deductedItemsCount >= stateSDF.totalItemsToDeduct) {
                                orderBus.send(new MarkCheckoutCompletedCommand(stateSDF.orderId));
                            }
                        }
                        break;

                    case "OrderCreated":
                        OrderCreated oc = objectMapper.readValue(message, OrderCreated.class);
                        SagaState stateOC = activeSagas.get(UUID.fromString(oc.orderId()));
                        if (stateOC != null) {
                            cartBus.send(new ClearCartCommand(stateOC.guestToken));
                        }
                        break;

                    case "CartCleared":
                        CartCleared cc = objectMapper.readValue(message, CartCleared.class);
                        SagaState stateCC = activeSagas.values().stream()
                                .filter(s -> s.guestToken.equals(cc.guestToken()))
                                .findFirst().orElse(null);
                        if (stateCC != null) {
                            orderBus.send(new MarkCheckoutCompletedCommand(stateCC.orderId));
                            activeSagas.remove(stateCC.orderId);
                            System.out.println("Saga SUCCESSFULLY COMPLETED for order: " + stateCC.orderId);
                        }
                        break;

                    default:
                        System.err.println("No handler for message type: " + messageType);
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }
}
