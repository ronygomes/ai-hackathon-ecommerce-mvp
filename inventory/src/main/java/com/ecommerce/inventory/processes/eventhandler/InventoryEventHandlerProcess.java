package com.ecommerce.inventory.processes.eventhandler;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.inventory.domain.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Updates;
import com.rabbitmq.client.*;
import com.ecommerce.checkout.saga.messages.events.StockDeductedForOrder;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class InventoryEventHandlerProcess {
    public static void main(String[] args) throws Exception {
        MongoClient mongoClient = new MongoClientProvider().get();
        MongoDatabase database = mongoClient.getDatabase("aihackathon");

        MongoCollection<Document> stockAvailabilityCollection = database.getCollection("stock_availability_view");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "inventory_events";
        channel.exchangeDeclare(exchangeName, "fanout", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeName, "");

        ObjectMapper objectMapper = new ObjectMapper();

        System.out.println("InventoryEventHandler waiting for events on " + exchangeName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("StockItemCreated".equals(messageType) || "StockSet".equals(messageType)
                        || "StockDeductedForOrder".equals(messageType)) {
                    // All these events update the same stock availability view
                    UUID productId;
                    int newQty;

                    if ("StockItemCreated".equals(messageType)) {
                        StockItemCreated event = objectMapper.readValue(message, StockItemCreated.class);
                        productId = event.productId();
                        newQty = event.initialQty();
                    } else if ("StockSet".equals(messageType)) {
                        StockSet event = objectMapper.readValue(message, StockSet.class);
                        productId = event.productId();
                        newQty = event.newQty();
                    } else {
                        StockDeductedForOrder event = objectMapper.readValue(message, StockDeductedForOrder.class);
                        productId = event.productId();
                        newQty = event.newQty();
                    }

                    String pid = productId.toString();
                    Document stockDoc = new Document()
                            .append("_id", pid)
                            .append("productId", pid)
                            .append("availableQty", newQty)
                            .append("inStockFlag", newQty > 0);

                    stockAvailabilityCollection.replaceOne(eq("_id", pid), stockDoc, new ReplaceOptions().upsert(true));
                    System.out.println("Updated stock availability for " + pid + " to " + newQty);
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
