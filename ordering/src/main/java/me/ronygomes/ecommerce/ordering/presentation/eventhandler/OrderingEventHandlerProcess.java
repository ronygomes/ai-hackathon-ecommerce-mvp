package me.ronygomes.ecommerce.ordering.presentation.eventhandler;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.rabbitmq.client.*;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.ordering.presentation.eventhandler.handler.OrderCreatedProjectionHandler;
import org.bson.Document;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class OrderingEventHandlerProcess {
    static void main() throws Exception {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoCollection<Document> collection = mongoClient.getDatabase("aihackathon")
                .getCollection("order_projections");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "ordering_events";
        String queueName = "ordering_projection_service";
        channel.queueDeclare(queueName, true, false, false, null);
        channel.queueBind(queueName, exchangeName, "");

        ObjectMapper objectMapper = new ObjectMapper();
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(objectMapper);

        // Register handlers
        dispatcher.registerHandler("OrderCreated", new OrderCreatedProjectionHandler(collection));

        System.out.println("Ordering EventHandler waiting for events (Dispatcher Pattern) on " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.get("X-Message-Type") != null
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
