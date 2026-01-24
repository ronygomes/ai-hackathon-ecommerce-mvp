package com.ecommerce.ordering.processes.commandhandler;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.ordering.application.*;
import com.ecommerce.ordering.domain.Order;
import com.ecommerce.ordering.domain.OrderId;
import com.ecommerce.ordering.infrastructure.MongoOrderRepository;
import com.ecommerce.ordering.infrastructure.IOrderRepository;
import com.ecommerce.ordering.infrastructure.OrderingMessageBus;
import tools.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.*;

import java.util.Map;
import java.util.UUID;

public class OrderingCommandHandlerProcess {
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new OrderingModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "ordering_commands";
        channel.queueDeclare(queueName, true, false, false, null);

        ObjectMapper objectMapper = new ObjectMapper();
        ICommandHandler<PlaceOrderCommand, UUID> placeOrderHandler = injector.getInstance(PlaceOrderHandler.class);

        System.out.println("OrderingCommandHandler waiting for messages on " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("PlaceOrderCommand".equals(messageType)) {
                    PlaceOrderCommand command = objectMapper.readValue(message, PlaceOrderCommand.class);
                    placeOrderHandler.handle(command).get();
                } else {
                    System.err.println("Unknown command type: " + messageType);
                }
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }

    static class OrderingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(com.mongodb.client.MongoClient.class)
                    .toProvider(com.ecommerce.core.infrastructure.MongoClientProvider.class);
            bind(IOrderRepository.class).to(MongoOrderRepository.class);
            bind(com.google.inject.Key.get(new TypeLiteral<IRepository<Order, OrderId>>() {
            })).to(MongoOrderRepository.class);
            bind(IMessageBus.class).to(OrderingMessageBus.class);
        }
    }
}
