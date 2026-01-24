package com.ecommerce.inventory.processes.commandhandler;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.inventory.application.*;
import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.domain.ProductId;
import com.ecommerce.inventory.infrastructure.MongoInventoryRepository;
import com.ecommerce.inventory.infrastructure.InventoryMessageBus;
import tools.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.*;

import java.util.Map;

public class InventoryCommandHandlerProcess {
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new InventoryModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "inventory_commands";
        channel.queueDeclare(queueName, true, false, false, null);

        ObjectMapper objectMapper = new ObjectMapper();
        ICommandHandler<SetStockCommand, Void> setStockHandler = injector.getInstance(SetStockHandler.class);
        ICommandHandler<DeductStockForOrderCommand, Void> deductStockHandler = injector
                .getInstance(DeductStockForOrderHandler.class);

        System.out.println("InventoryCommandHandler waiting for messages on " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("SetStockCommand".equals(messageType)) {
                    SetStockCommand command = objectMapper.readValue(message, SetStockCommand.class);
                    setStockHandler.handle(command).get();
                } else if ("DeductStockForOrderCommand".equals(messageType)) {
                    DeductStockForOrderCommand command = objectMapper.readValue(message,
                            DeductStockForOrderCommand.class);
                    deductStockHandler.handle(command).get();
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

    static class InventoryModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MongoClient.class).toProvider(MongoClientProvider.class);
            bind(new TypeLiteral<IRepository<InventoryItem, ProductId>>() {
            }).to(MongoInventoryRepository.class);
            bind(IMessageBus.class).to(InventoryMessageBus.class);
        }
    }
}
