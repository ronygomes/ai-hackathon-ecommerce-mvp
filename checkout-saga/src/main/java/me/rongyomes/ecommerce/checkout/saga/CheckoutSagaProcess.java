package me.rongyomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.infrastructure.RabbitMQCommandBus;

import java.util.Map;

public class CheckoutSagaProcess {

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
        SagaStateStore store = new MongoSagaStateStore(new MongoClientProvider().get());
        SagaOrchestrator orchestrator = new SagaOrchestrator(orderBus, cartBus, catalogBus, inventoryBus, objectMapper, store);

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
