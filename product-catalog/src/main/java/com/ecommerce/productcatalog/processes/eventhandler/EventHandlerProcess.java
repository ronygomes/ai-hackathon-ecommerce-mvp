package com.ecommerce.productcatalog.processes.eventhandler;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.productcatalog.domain.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.rabbitmq.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

public class EventHandlerProcess {
    public static void main(String[] args) throws Exception {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");

        MongoCollection<Document> listViewCollection = database.getCollection("product_list_view");
        MongoCollection<Document> detailViewCollection = database.getCollection("product_detail_view");

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
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("ProductCreated".equals(messageType)) {
                    ProductCreated event = objectMapper.readValue(message, ProductCreated.class);
                    String pid = event.productId().toString();

                    Document listDoc = new Document()
                            .append("_id", pid)
                            .append("productId", pid)
                            .append("name", event.name().value())
                            .append("price", event.price().value())
                            .append("isActive", false);
                    listViewCollection.replaceOne(eq("_id", pid), listDoc, new ReplaceOptions().upsert(true));

                    Document detailDoc = new Document()
                            .append("_id", pid)
                            .append("sku", event.sku().value())
                            .append("name", event.name().value())
                            .append("description", event.description().value())
                            .append("price", event.price().value())
                            .append("isActive", false);
                    detailViewCollection.replaceOne(eq("_id", pid), detailDoc, new ReplaceOptions().upsert(true));

                    System.out.println("Handled ProductCreated for " + pid);
                } else if ("ProductDetailsUpdated".equals(messageType)) {
                    ProductDetailsUpdated event = objectMapper.readValue(message, ProductDetailsUpdated.class);
                    String pid = event.productId().toString();

                    listViewCollection.updateOne(eq("_id", pid), Updates.set("name", event.name().value()));

                    Bson detailUpdates = Updates.combine(
                            Updates.set("name", event.name().value()),
                            Updates.set("description", event.description().value()));
                    detailViewCollection.updateOne(eq("_id", pid), detailUpdates);

                    System.out.println("Handled ProductDetailsUpdated for " + pid);
                } else if ("ProductPriceChanged".equals(messageType)) {
                    ProductPriceChanged event = objectMapper.readValue(message, ProductPriceChanged.class);
                    String pid = event.productId().toString();
                    double newPrice = event.newPrice().value();

                    listViewCollection.updateOne(eq("_id", pid), Updates.set("price", newPrice));
                    detailViewCollection.updateOne(eq("_id", pid), Updates.set("price", newPrice));

                    System.out.println("Handled ProductPriceChanged for " + pid);
                } else if ("ProductActivated".equals(messageType)) {
                    ProductActivated event = objectMapper.readValue(message, ProductActivated.class);
                    String pid = event.productId().toString();

                    listViewCollection.updateOne(eq("_id", pid), Updates.set("isActive", true));
                    detailViewCollection.updateOne(eq("_id", pid), Updates.set("isActive", true));

                    System.out.println("Handled ProductActivated for " + pid);
                } else if ("ProductDeactivated".equals(messageType)) {
                    ProductDeactivated event = objectMapper.readValue(message, ProductDeactivated.class);
                    String pid = event.productId().toString();

                    listViewCollection.updateOne(eq("_id", pid), Updates.set("isActive", false));
                    detailViewCollection.updateOne(eq("_id", pid), Updates.set("isActive", false));

                    System.out.println("Handled ProductDeactivated for " + pid);
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
