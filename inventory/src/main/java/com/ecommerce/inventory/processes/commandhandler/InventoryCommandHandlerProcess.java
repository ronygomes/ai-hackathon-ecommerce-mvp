package com.ecommerce.inventory.processes.commandhandler;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.core.messaging.MessageDispatcher;
import com.ecommerce.core.messaging.CommandHandlerDispatcherAdapter;
import com.ecommerce.inventory.application.*;
import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.domain.ProductId;
import com.ecommerce.inventory.infrastructure.MongoInventoryRepository;
import com.ecommerce.inventory.infrastructure.InventoryMessageBus;
import com.ecommerce.checkout.saga.messages.commands.ValidateStockBatchCommand;
import com.ecommerce.checkout.saga.messages.commands.DeductStockForOrderCommand;
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
        MessageDispatcher dispatcher = new MessageDispatcher(objectMapper);

        // Register handlers using Adapter
        dispatcher.registerHandler("SetStockCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(SetStockHandler.class),
                        SetStockCommand.class));
        dispatcher.registerHandler("ValidateStockCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(ValidateStockHandler.class),
                        ValidateStockCommand.class));
        dispatcher.registerHandler("ValidateStockBatchCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(ValidateStockBatchHandler.class),
                        ValidateStockBatchCommand.class));
        dispatcher.registerHandler("DeductStockForOrderCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(DeductStockForOrderHandler.class),
                        DeductStockForOrderCommand.class));

        System.out.println("InventoryCommandHandler waiting for messages (Dispatcher Pattern) on " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            dispatcher.dispatch(messageType, message)
                    .thenRun(() -> {
                        try {
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    })
                    .exceptionally(e -> {
                        e.printStackTrace();
                        return null;
                    });
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }

    static class InventoryModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MongoClient.class).toProvider(MongoClientProvider.class);
            bind(com.google.inject.Key.get(new TypeLiteral<IRepository<InventoryItem, ProductId>>() {
            })).to(MongoInventoryRepository.class);
            bind(IMessageBus.class).to(InventoryMessageBus.class);
        }
    }
}
