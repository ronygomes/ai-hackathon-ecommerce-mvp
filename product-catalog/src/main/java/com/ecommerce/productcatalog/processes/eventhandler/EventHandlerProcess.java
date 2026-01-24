package com.ecommerce.productcatalog.processes.eventhandler;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.productcatalog.domain.ProductCreatedEvent;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import static com.mongodb.client.model.Filters.eq;

public class EventHandlerProcess {
    public static void main(String[] args) throws Exception {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");
        MongoCollection<Document> productsCollection = database.getCollection("products_view");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "product_catalog_events";
        channel.exchangeDeclare(exchangeName, "fanout", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeName, "");

        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("EventHandler waiting for events on " + exchangeName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            try {
                ProductCreatedEvent event = objectMapper.readValue(message, ProductCreatedEvent.class);
                Document doc = new Document()
                        .append("_id", event.productId().toString())
                        .append("name", event.name())
                        .append("sku", event.sku())
                        .append("price", event.price());
                productsCollection.replaceOne(eq("_id", event.productId().toString()), doc,
                        new ReplaceOptions().upsert(true));
                System.out.println("Handled ProductCreatedEvent for " + event.productId());
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }
}
