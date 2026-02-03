package me.ronygomes.ecommerce.inventory.presentation.commandhandler;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.*;
import me.rongyomes.ecommerce.checkout.saga.message.command.DeductStockForOrderCommand;
import me.rongyomes.ecommerce.checkout.saga.message.command.ValidateStockBatchCommand;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.CommandHandlerDispatcherAdapter;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.inventory.application.*;
import me.ronygomes.ecommerce.inventory.domain.InventoryItem;
import me.ronygomes.ecommerce.inventory.domain.ProductId;
import me.ronygomes.ecommerce.inventory.infrastructure.InventoryMessageBus;
import me.ronygomes.ecommerce.inventory.infrastructure.MongoInventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class InventoryCommandHandlerProcess {
    static void main() throws Exception {
        Injector injector = Guice.createInjector(new InventoryModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "inventory_commands";
        channel.queueDeclare(queueName, true, false, false, null);

        ObjectMapper objectMapper = new ObjectMapper();
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(objectMapper);

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
            bind(com.google.inject.Key.get(new TypeLiteral<Repository<InventoryItem, ProductId>>() {
            })).to(MongoInventoryRepository.class);
            bind(MessageBus.class).to(InventoryMessageBus.class);
        }
    }
}
