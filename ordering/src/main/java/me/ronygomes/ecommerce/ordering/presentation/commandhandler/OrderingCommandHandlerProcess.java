package me.ronygomes.ecommerce.ordering.presentation.commandhandler;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.rabbitmq.client.*;
import me.rongyomes.ecommerce.checkout.saga.message.command.MarkCheckoutCompletedCommand;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.CommandHandlerDispatcherAdapter;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.ordering.application.MarkCheckoutCompletedHandler;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderHandler;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;
import me.ronygomes.ecommerce.ordering.infrastructure.MongoOrderRepository;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderRepository;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderingMessageBus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class OrderingCommandHandlerProcess {
    static void main() throws Exception {
        Injector injector = Guice.createInjector(new OrderingModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "ordering_commands";
        channel.queueDeclare(queueName, true, false, false, null);

        ObjectMapper objectMapper = new ObjectMapper();
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(objectMapper);

        // Register handlers using Adapter
        dispatcher.registerHandler("PlaceOrderCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(PlaceOrderHandler.class),
                        PlaceOrderCommand.class));
        dispatcher.registerHandler("MarkCheckoutCompletedCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(MarkCheckoutCompletedHandler.class),
                        MarkCheckoutCompletedCommand.class));

        System.out.println("Ordering CommandHandler waiting for messages (Dispatcher Pattern) on " + queueName);

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

    static class OrderingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(com.mongodb.client.MongoClient.class)
                    .toProvider(MongoClientProvider.class);
            bind(OrderRepository.class).to(MongoOrderRepository.class);
            bind(com.google.inject.Key.get(new TypeLiteral<Repository<Order, OrderId>>() {
            })).to(MongoOrderRepository.class);
            bind(MessageBus.class).to(OrderingMessageBus.class);
        }
    }
}
