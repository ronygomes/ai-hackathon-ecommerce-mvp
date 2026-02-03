package me.ronygomes.ecommerce.cart.process.commandhandler;

import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.core.messaging.CommandHandlerDispatcherAdapter;
import me.ronygomes.ecommerce.cart.application.*;
import me.ronygomes.ecommerce.cart.infrastructure.MongoCartRepository;
import me.ronygomes.ecommerce.cart.infrastructure.CartRepository;
import me.ronygomes.ecommerce.cart.infrastructure.CartMessageBus;
import me.rongyomes.ecommerce.checkout.saga.message.command.ClearCartCommand;
import me.rongyomes.ecommerce.checkout.saga.message.command.GetCartSnapshotCommand;
import tools.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.*;

import java.util.Map;

public class CartCommandHandlerProcess {
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new CartModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "cart_commands";
        channel.queueDeclare(queueName, true, false, false, null);

        ObjectMapper objectMapper = new ObjectMapper();
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(objectMapper);

        // Register handlers using Adapter
        dispatcher.registerHandler("AddCartItemCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(AddCartItemHandler.class),
                        AddCartItemCommand.class));
        dispatcher.registerHandler("RemoveCartItemCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(RemoveCartItemHandler.class),
                        RemoveCartItemCommand.class));
        dispatcher.registerHandler("UpdateCartItemQtyCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(UpdateCartItemQtyHandler.class),
                        UpdateCartItemQtyCommand.class));
        dispatcher.registerHandler("ClearCartCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(ClearCartHandler.class),
                        ClearCartCommand.class));
        dispatcher.registerHandler("GetCartSnapshotCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(GetCartSnapshotHandler.class),
                        GetCartSnapshotCommand.class));

        System.out.println("CartCommandHandler waiting for messages (Dispatcher Pattern) on " + queueName);

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

    static class CartModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MongoClient.class).toProvider(MongoClientProvider.class);
            bind(CartRepository.class).to(MongoCartRepository.class);
            bind(MessageBus.class).to(CartMessageBus.class);
        }
    }
}
