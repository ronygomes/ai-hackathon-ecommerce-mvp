package me.ronygomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.mongodb.client.MongoClient;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.MongoProcessedCommandStore;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.ProcessedCommandStore;

import java.util.Map;

public class CheckoutSagaProcess {

    static void main() throws Exception {
        AppConfig config = AppConfig.fromEnv();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.rabbitHost());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        CommandBus orderBus = new RabbitMQCommandBus("ordering_commands", config.rabbitHost());
        CommandBus cartBus = new RabbitMQCommandBus("cart_commands", config.rabbitHost());
        CommandBus catalogBus = new RabbitMQCommandBus("product_catalog_commands", config.rabbitHost());
        CommandBus inventoryBus = new RabbitMQCommandBus("inventory_commands", config.rabbitHost());

        ObjectMapper objectMapper = new ObjectMapper();
        MongoClient mongoClient = new MongoClientProvider(config).get();
        SagaStateStore store = new MongoSagaStateStore(mongoClient, config.mongoDbName());
        ProcessedCommandStore processedEventStore = new MongoProcessedCommandStore(
                mongoClient, config.mongoDbName(), "saga_processed_events");
        SagaOrchestrator orchestrator = new SagaOrchestrator(
                orderBus, cartBus, catalogBus, inventoryBus, objectMapper, store, processedEventStore);

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
                orchestrator.handle(messageType, message);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }
}
