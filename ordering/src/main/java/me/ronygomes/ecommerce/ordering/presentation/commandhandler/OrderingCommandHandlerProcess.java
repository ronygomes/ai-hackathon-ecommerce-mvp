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
import me.ronygomes.ecommerce.checkout.saga.message.command.CancelOrderCommand;
import me.ronygomes.ecommerce.checkout.saga.message.command.MarkCheckoutCompletedCommand;
import me.ronygomes.ecommerce.core.infrastructure.AppConfig;
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
import me.ronygomes.ecommerce.ordering.application.CancelOrderHandler;
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

    private static final String OUTBOX_COLLECTION = "ordering_outbox";
    private static final String PROCESSED_COMMANDS_COLLECTION = "ordering_processed_commands";
    private static final int OUTBOX_BATCH_SIZE = 100;
    private static final long OUTBOX_TICK_INTERVAL_MS = 500;

    static void main() throws Exception {
        AppConfig config = AppConfig.fromEnv();
        Injector injector = Guice.createInjector(new OrderingModule(config));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.rabbitHost());
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
        dispatcher.registerHandler("CancelOrderCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(CancelOrderHandler.class),
                        CancelOrderCommand.class, processedCommandStore));

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
        private final AppConfig config;

        OrderingModule(AppConfig config) {
            this.config = config;
        }

        @Override
        protected void configure() {
            bind(AppConfig.class).toInstance(config);
            bind(MongoClient.class).toProvider(MongoClientProvider.class);
            bind(OrderRepository.class).to(MongoOrderRepository.class);
            bind(com.google.inject.Key.get(new TypeLiteral<Repository<Order, OrderId>>() {
            })).to(MongoOrderRepository.class);
            bind(MessageBus.class).to(OrderingMessageBus.class);
        }

        @Provides
        @Singleton
        OutboxStore outboxStore(MongoClient mongoClient, AppConfig config) {
            return new MongoOutboxStore(mongoClient, config.mongoDbName(), OUTBOX_COLLECTION);
        }

        @Provides
        @Singleton
        ProcessedCommandStore processedCommandStore(MongoClient mongoClient, AppConfig config) {
            return new MongoProcessedCommandStore(mongoClient, config.mongoDbName(), PROCESSED_COMMANDS_COLLECTION);
        }
    }
}
