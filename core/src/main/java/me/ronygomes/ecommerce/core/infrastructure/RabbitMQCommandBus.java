package me.ronygomes.ecommerce.core.infrastructure;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandBus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RabbitMQCommandBus implements CommandBus {
    private final String queueName;
    private final ObjectMapper objectMapper;
    private final ConnectionFactory factory;

    public RabbitMQCommandBus(String queueName) {
        this(queueName, "localhost");
    }

    public RabbitMQCommandBus(String queueName, String host) {
        this(queueName, defaultFactory(host));
    }

    public RabbitMQCommandBus(String queueName, ConnectionFactory factory) {
        this.queueName = queueName;
        this.objectMapper = new ObjectMapper();
        this.factory = factory;
    }

    private static ConnectionFactory defaultFactory(String host) {
        ConnectionFactory f = new ConnectionFactory();
        f.setHost(host);
        return f;
    }

    @Override
    public <TResponse> CompletableFuture<TResponse> send(Command<TResponse> command) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = factory.newConnection();
                    Channel channel = connection.createChannel()) {
                channel.queueDeclare(queueName, true, false, false, null);
                byte[] message = objectMapper.writeValueAsBytes(command);

                Map<String, Object> headers = new HashMap<>();
                headers.put("X-Message-Type", command.getClass().getSimpleName());
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                        .headers(headers)
                        .build();

                channel.basicPublish("", queueName, props, message);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send command to RabbitMQ", e);
            }
        }).thenApply(v -> null); // Command API returns 202, doesn't wait for internal result in this simple MVP
    }
}
