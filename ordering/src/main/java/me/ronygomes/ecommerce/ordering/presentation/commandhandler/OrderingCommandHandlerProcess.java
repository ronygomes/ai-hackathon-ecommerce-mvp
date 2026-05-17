package me.ronygomes.ecommerce.ordering.presentation.commandhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import me.rongyomes.ecommerce.checkout.saga.message.command.MarkCheckoutCompletedCommand;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.MongoProcessedCommandStore;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.ProcessedCommandStore;
import me.ronygomes.ecommerce.core.infrastructure.outbox.MongoOutboxStore;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxDispatcher;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.core.messaging.CommandHandlerDispatcherAdapter;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.core.messaging.MessageMetadata;
import me.ronygomes.ecommerce.ordering.application.MarkCheckoutCompletedHandler;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderCommand;
import me.ronygomes.ecommerce.ordering.application.PlaceOrderHandler;
import me.ronygomes.ecommerce.ordering.domain.Order;
import me.ronygomes.ecommerce.ordering.domain.OrderId;
import me.ronygomes.ecommerce.ordering.infrastructure.MongoOrderRepository;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderRepository;
import me.ronygomes.ecommerce.ordering.infrastructure.OrderingMessageBus;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderingCommandHandlerProcess {

    private static final String DB_NAME = "aihackathon";
    private static final String OUTBOX_COLLECTION = "ordering_outbox";
    private static final String PROCESSED_COMMANDS_COLLECTION = "ordering_processed_commands";
    private static final int OUTBOX_BATCH_SIZE = 100;
    private static final long OUTBOX_TICK_INTERVAL_MS = 500;

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
        ProcessedCommandStore processedCommandStore = injector.getInstance(ProcessedCommandStore.class);

        dispatcher.registerHandler("PlaceOrderCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(PlaceOrderHandler.class),
                        PlaceOrderCommand.class, processedCommandStore));
        dispatcher.registerHandler("MarkCheckoutCompletedCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(MarkCheckoutCompletedHandler.class),
                        MarkCheckoutCompletedCommand.class, processedCommandStore));

        OutboxDispatcher outboxDispatcher = new OutboxDispatcher(
                injector.getInstance(OutboxStore.class),
                injector.getInstance(MessageBus.class),
                OUTBOX_BATCH_SIZE);
        ScheduledExecutorService outboxScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ordering-outbox-dispatcher");
            t.setDaemon(true);
            return t;
        });
        outboxScheduler.scheduleWithFixedDelay(() -> {
            try {
                outboxDispatcher.tick();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, OUTBOX_TICK_INTERVAL_MS, OUTBOX_TICK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        System.out.println("Ordering CommandHandler waiting for messages (Dispatcher Pattern) on " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";
            String commandId = headers != null && headers.get("X-Command-Id") != null
                    ? headers.get("X-Command-Id").toString()
                    : null;

            dispatcher.dispatch(messageType, message, MessageMetadata.withCommandId(commandId))
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
            bind(MongoClient.class).toProvider(MongoClientProvider.class);
            bind(OrderRepository.class).to(MongoOrderRepository.class);
            bind(com.google.inject.Key.get(new TypeLiteral<Repository<Order, OrderId>>() {
            })).to(MongoOrderRepository.class);
            bind(MessageBus.class).to(OrderingMessageBus.class);
        }

        @Provides
        @Singleton
        OutboxStore outboxStore(MongoClient mongoClient) {
            return new MongoOutboxStore(mongoClient, DB_NAME, OUTBOX_COLLECTION);
        }

        @Provides
        @Singleton
        ProcessedCommandStore processedCommandStore(MongoClient mongoClient) {
            return new MongoProcessedCommandStore(mongoClient, DB_NAME, PROCESSED_COMMANDS_COLLECTION);
        }
    }
}
