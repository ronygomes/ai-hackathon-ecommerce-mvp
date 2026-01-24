package com.ecommerce.ordering.processes.eventhandler;

import tools.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.checkout.saga.messages.events.OrderCreated;
import com.rabbitmq.client.*;
import org.bson.Document;
import java.util.Map;

public class OrderingEventHandlerProcess {
    public static void main(String[] args) throws Exception {
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

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.get("X-Message-Type") != null
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("OrderCreated".equals(messageType)) {
                    OrderCreated event = objectMapper.readValue(message, OrderCreated.class);
                    Document doc = new Document("_id", event.orderId())
                            .append("guestToken", event.guestToken())
                            .append("status", "SUBMITTED")
                            .append("customerEmail", event.customerEmail());
                    collection.insertOne(doc);
                    System.out.println("Projected order: " + event.orderId());
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
