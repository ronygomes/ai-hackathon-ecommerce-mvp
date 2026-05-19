package me.ronygomes.ecommerce.productcatalog.presentation.eventhandler;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.*;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.core.messaging.MessageMetadata;
import me.ronygomes.ecommerce.productcatalog.presentation.eventhandler.handler.*;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class EventHandlerProcess {

    private static final Logger log = LoggerFactory.getLogger(EventHandlerProcess.class);

    static void main() throws Exception {
        AppConfig config = AppConfig.fromEnv();
        MongoClient mongoClient = new MongoClientProvider(config).get();
        MongoDatabase database = mongoClient.getDatabase(config.mongoDbName());

        MongoCollection<Document> listViewCollection = database.getCollection("product_list_view");
        MongoCollection<Document> detailViewCollection = database.getCollection("product_detail_view");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.rabbitHost());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "product_catalog_events";
        channel.exchangeDeclare(exchangeName, "fanout", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeName, "");

        ObjectMapper objectMapper = new ObjectMapper();
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(objectMapper);

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

        log.info("ProductCatalog EventHandler waiting for events on {}", exchangeName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            dispatcher.dispatch(messageType, message, MessageMetadata.empty())
                    .thenRun(() -> {
                        try {
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        } catch (Exception e) {
                            log.error("Failed to ack delivery (messageType={})", messageType, e);
                        }
                    })
                    .exceptionally(e -> {
                        log.error("Projection handler failed (messageType={})", messageType, e);
                        return null;
                    });
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }
}
