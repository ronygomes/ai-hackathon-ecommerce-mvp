package com.ecommerce.checkout.saga;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.infrastructure.RabbitMQCommandBus;
import com.ecommerce.core.messaging.MessageDispatcher;
import com.ecommerce.checkout.saga.handlers.*;
import tools.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
        MessageDispatcher dispatcher = new MessageDispatcher(objectMapper);

        // Register handlers
        dispatcher.registerHandler("CheckoutRequested", new CheckoutRequestedHandler(activeSagas, cartBus));
        dispatcher.registerHandler("CartSnapshotProvided", new CartSnapshotProvidedHandler(activeSagas, catalogBus));
        dispatcher.registerHandler("ProductSnapshotsProvided",
                new ProductSnapshotsProvidedHandler(activeSagas, inventoryBus));
        dispatcher.registerHandler("StockBatchValidated", new StockBatchValidatedHandler(activeSagas, inventoryBus));
        dispatcher.registerHandler("StockDeductedForOrder", new SagaStockDeductedHandler(activeSagas, orderBus));
        dispatcher.registerHandler("OrderCreated", new SagaOrderCreatedHandler(activeSagas, cartBus));
        dispatcher.registerHandler("CartCleared", new SagaCartClearedHandler(activeSagas, orderBus));

        String queueName = "checkout_saga_coordinator";
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, "ordering_events", "");
        channel.queueBind(queueName, "cart_events", "");
        channel.queueBind(queueName, "product_catalog_events", "");
        channel.queueBind(queueName, "inventory_events", "");

        System.out.println("CheckoutSaga Coordinator waiting for events (Dispatcher Pattern)...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = (headers != null && headers.get("X-Message-Type") != null)
                    ? headers.get("X-Message-Type").toString()
                    : "";

            dispatcher.dispatch(messageType, message)
                    .thenRun(() -> {
                        try {
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }
}
