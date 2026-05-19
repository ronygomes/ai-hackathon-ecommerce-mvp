package me.ronygomes.ecommerce.ordering.presentation.eventhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.rabbitmq.client.*;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.core.messaging.MessageMetadata;
import me.ronygomes.ecommerce.ordering.presentation.eventhandler.handler.OrderCreatedProjectionHandler;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class OrderingEventHandlerProcess {

    private static final Logger log = LoggerFactory.getLogger(OrderingEventHandlerProcess.class);

    static void main() throws Exception {
        AppConfig config = AppConfig.fromEnv();
        MongoClient mongoClient = new MongoClientProvider(config).get();
        MongoCollection<Document> collection = mongoClient.getDatabase(config.mongoDbName())
                .getCollection("order_projections");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.rabbitHost());
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

        log.info("Ordering EventHandler waiting for events on {}", queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.get(Command.HEADER_MESSAGE_TYPE) != null
                    ? headers.get(Command.HEADER_MESSAGE_TYPE).toString()
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
