package me.ronygomes.ecommerce.core.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandBus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RabbitMQCommandBus implements CommandBus {

    private static final String DEFAULT_EXCHANGE_NAME = "";

    private final String queueName;
    private final ObjectMapper objectMapper;
    private final ConnectionFactory factory;

    public RabbitMQCommandBus(String queueName, String host) {
        this(queueName, defaultFactory(host), new ObjectMapper());
    }

    public RabbitMQCommandBus(String queueName, ConnectionFactory factory, ObjectMapper objectMapper) {
        this.queueName = queueName;
        this.objectMapper = objectMapper;
        this.factory = factory;
    }

    @Override
    public CompletableFuture<UUID> send(Command<?> command) {
        UUID commandId = UUID.randomUUID();

        return CompletableFuture.runAsync(() -> {
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.queueDeclare(queueName, true, false, false, null);
                byte[] message = objectMapper.writeValueAsBytes(command);

                Map<String, Object> headers = new HashMap<>();
                headers.put(Command.HEADER_MESSAGE_TYPE, command.getClass().getSimpleName());
                headers.put(Command.HEADER_COMMAND_ID, commandId.toString());
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                        .headers(headers)
                        .build();

                channel.basicPublish(DEFAULT_EXCHANGE_NAME, queueName, props, message);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send command to RabbitMQ", e);
            }
        }).thenApply(v -> commandId);
    }

    private static ConnectionFactory defaultFactory(String host) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        return factory;
    }
}
