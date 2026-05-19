package me.ronygomes.ecommerce.cart.presentation.eventhandler;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.*;
import me.ronygomes.ecommerce.cart.presentation.eventhandler.handler.*;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.core.messaging.MessageMetadata;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class CartEventHandlerProcess {

    private static final Logger log = LoggerFactory.getLogger(CartEventHandlerProcess.class);

    static void main() throws Exception {
        AppConfig config = AppConfig.fromEnv();
        MongoClient mongoClient = new MongoClientProvider(config).get();
        MongoDatabase database = mongoClient.getDatabase(config.mongoDbName());
        MongoCollection<Document> cartViewCollection = database.getCollection("cart_view");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.rabbitHost());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String exchangeName = "cart_events";
        channel.exchangeDeclare(exchangeName, "fanout", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchangeName, "");

        ObjectMapper objectMapper = new ObjectMapper();
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(objectMapper);

        // Register handlers
        dispatcher.registerHandler("CartCreated", new CartCreatedHandler(cartViewCollection));
        dispatcher.registerHandler("CartItemAdded", new CartItemAddedHandler(cartViewCollection));
        dispatcher.registerHandler("CartItemQuantityUpdated", new CartItemQuantityUpdatedHandler(cartViewCollection));
        dispatcher.registerHandler("CartItemRemoved", new CartItemRemovedHandler(cartViewCollection));
        dispatcher.registerHandler("CartCleared", new CartClearedEventProjectionHandler(cartViewCollection));

        log.info("CartEventHandler waiting for events on {}", exchangeName);

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
