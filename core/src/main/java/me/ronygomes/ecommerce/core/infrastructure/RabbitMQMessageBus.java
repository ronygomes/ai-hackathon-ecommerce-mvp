package me.ronygomes.ecommerce.core.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import me.ronygomes.ecommerce.core.messaging.MessageBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RabbitMQMessageBus implements MessageBus {

    private final String exchangeName;
    private final ObjectMapper objectMapper;
    private final ConnectionFactory factory;

    public RabbitMQMessageBus(String exchangeName, String host) {
        this(exchangeName, defaultFactory(host), new ObjectMapper());
    }

    public RabbitMQMessageBus(String exchangeName, ConnectionFactory factory, ObjectMapper objectMapper) {
        this.exchangeName = exchangeName;
        this.objectMapper = objectMapper;
        this.factory = factory;
    }

    private static ConnectionFactory defaultFactory(String host) {
        ConnectionFactory f = new ConnectionFactory();
        f.setHost(host);
        return f;
    }

    @Override
    public CompletableFuture<Void> publish(List<DomainEvent> events) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(exchangeName, "fanout", true);
                for (DomainEvent event : events) {
                    byte[] message = objectMapper.writeValueAsBytes(event);

                    Map<String, Object> headers = new HashMap<>();
                    headers.put(Command.HEADER_MESSAGE_TYPE, event.getClass().getSimpleName());
                    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                            .headers(headers)
                            .build();

                    channel.basicPublish(exchangeName, "", props, message);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to publish events to RabbitMQ", e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> publishRaw(List<RawMessage> messages) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(exchangeName, "fanout", true);
                for (RawMessage msg : messages) {
                    Map<String, Object> headers = new HashMap<>();
                    headers.put(Command.HEADER_MESSAGE_TYPE, msg.type());
                    AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                            .headers(headers)
                            .build();

                    channel.basicPublish(exchangeName, "", props, msg.payload());
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to publish raw messages to RabbitMQ", e);
            }
        });
    }
}
