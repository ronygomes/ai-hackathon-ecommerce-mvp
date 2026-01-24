package com.ecommerce.productcatalog.processes.eventhandler;

import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.core.messaging.MessageDispatcher;
import com.ecommerce.productcatalog.processes.eventhandler.handlers.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.*;
import org.bson.Document;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

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
        MessageDispatcher dispatcher = new MessageDispatcher(objectMapper);

        // Register handlers
        dispatcher.registerHandler("ProductCreated",
                new ProductCreatedProjectionHandler(listViewCollection, detailViewCollection));
        dispatcher.registerHandler("ProductDetailsUpdated",
                new ProductDetailsUpdatedProjectionHandler(listViewCollection, detailViewCollection));
        dispatcher.registerHandler("ProductPriceChanged",
                new ProductPriceChangedProjectionHandler(listViewCollection, detailViewCollection));
        dispatcher.registerHandler("ProductActivated",
                new ProductActivatedProjectionHandler(listViewCollection, detailViewCollection));
        dispatcher.registerHandler("ProductDeactivated",
                new ProductDeactivatedProjectionHandler(listViewCollection, detailViewCollection));

        System.out.println("ProductCatalog EventHandler waiting for events (Dispatcher Pattern) on " + exchangeName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
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
