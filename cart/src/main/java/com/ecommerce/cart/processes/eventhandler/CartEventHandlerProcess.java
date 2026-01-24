package com.ecommerce.cart.processes.eventhandler;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.cart.domain.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.rabbitmq.client.*;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public class CartEventHandlerProcess {
    public static void main(String[] args) throws Exception {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");
        MongoCollection<Document> cartViewCollection = database.getCollection("cart_view");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "cart_events";
        channel.exchangeDeclare(exchangeName, "fanout", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeName, "");

        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("CartEventHandler waiting for events on " + exchangeName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("CartCreated".equals(messageType)) {
                    CartCreated event = objectMapper.readValue(message, CartCreated.class);
                    Document doc = new Document()
                            .append("_id", event.cartId().toString())
                            .append("cartId", event.cartId().toString())
                            .append("guestToken", event.guestToken())
                            .append("items", new ArrayList<>());
                    cartViewCollection.replaceOne(eq("_id", event.cartId().toString()), doc,
                            new ReplaceOptions().upsert(true));
                } else if ("CartItemAdded".equals(messageType)) {
                    CartItemAdded event = objectMapper.readValue(message, CartItemAdded.class);
                    Document item = new Document().append("productId", event.productId().toString()).append("qty",
                            event.qty());
                    cartViewCollection.updateOne(eq("_id", event.cartId().toString()), Updates.push("items", item));
                } else if ("CartItemQuantityUpdated".equals(messageType)) {
                    CartItemQuantityUpdated event = objectMapper.readValue(message, CartItemQuantityUpdated.class);
                    cartViewCollection.updateOne(
                            Filters.and(eq("_id", event.cartId().toString()),
                                    eq("items.productId", event.productId().toString())),
                            Updates.set("items.$.qty", event.newQty()));
                } else if ("CartItemRemoved".equals(messageType)) {
                    CartItemRemoved event = objectMapper.readValue(message, CartItemRemoved.class);
                    cartViewCollection.updateOne(
                            eq("_id", event.cartId().toString()),
                            Updates.pull("items", new Document("productId", event.productId().toString())));
                } else if ("CartCleared".equals(messageType)) {
                    CartCleared event = objectMapper.readValue(message, CartCleared.class);
                    cartViewCollection.updateOne(eq("_id", event.cartId().toString()),
                            Updates.set("items", new ArrayList<>()));
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
