package com.ecommerce.core.infrastructure;

import com.ecommerce.core.domain.IDomainEvent;
import com.ecommerce.core.messaging.IMessageBus;
import tools.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RabbitMQMessageBus implements IMessageBus {
    private final String exchangeName;
    private final ObjectMapper objectMapper;
    private final ConnectionFactory factory;

    public RabbitMQMessageBus(String exchangeName, String host) {
        this.exchangeName = exchangeName;
        this.objectMapper = new ObjectMapper();
        this.factory = new ConnectionFactory();
        this.factory.setHost(host);
    }

    @Override
    public CompletableFuture<Void> publish(List<IDomainEvent> events) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = factory.newConnection();
                    Channel channel = connection.createChannel()) {
                channel.exchangeDeclare(exchangeName, "fanout", true);
                for (IDomainEvent event : events) {
                    byte[] message = objectMapper.writeValueAsBytes(event);
                    channel.basicPublish(exchangeName, "", null, message);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to publish events to RabbitMQ", e);
            }
        });
    }
}
