package me.ronygomes.ecommerce.core.infrastructure;

import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandBus;
import tools.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class RabbitMQCommandBus implements CommandBus {
    private final String queueName;
    private final ObjectMapper objectMapper;
    private final ConnectionFactory factory;

    public RabbitMQCommandBus(String queueName, String host) {
        this.queueName = queueName;
        this.objectMapper = new ObjectMapper();
        this.factory = new ConnectionFactory();
        this.factory.setHost(host);
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
