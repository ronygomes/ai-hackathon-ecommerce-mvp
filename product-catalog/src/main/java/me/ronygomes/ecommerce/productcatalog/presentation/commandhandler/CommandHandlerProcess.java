package me.ronygomes.ecommerce.productcatalog.presentation.commandhandler;

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
import me.rongyomes.ecommerce.checkout.saga.message.command.GetProductSnapshotsCommand;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.infrastructure.outbox.MongoOutboxStore;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxDispatcher;
import me.ronygomes.ecommerce.core.infrastructure.outbox.OutboxStore;
import me.ronygomes.ecommerce.core.messaging.CommandHandlerDispatcherAdapter;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.core.messaging.MessageMetadata;
import me.ronygomes.ecommerce.productcatalog.application.ActivateProductCommand;
import me.ronygomes.ecommerce.productcatalog.application.ActivateProductHandler;
import me.ronygomes.ecommerce.productcatalog.application.ChangeProductPriceCommand;
import me.ronygomes.ecommerce.productcatalog.application.ChangeProductPriceHandler;
import me.ronygomes.ecommerce.productcatalog.application.CreateProductCommand;
import me.ronygomes.ecommerce.productcatalog.application.CreateProductHandler;
import me.ronygomes.ecommerce.productcatalog.application.DeactivateProductCommand;
import me.ronygomes.ecommerce.productcatalog.application.DeactivateProductHandler;
import me.ronygomes.ecommerce.productcatalog.application.GetProductSnapshotsHandler;
import me.ronygomes.ecommerce.productcatalog.application.UpdateProductDetailsCommand;
import me.ronygomes.ecommerce.productcatalog.application.UpdateProductDetailsHandler;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.infrastructure.MongoProductRepository;
import me.ronygomes.ecommerce.productcatalog.infrastructure.ProductCatalogMessageBus;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandHandlerProcess {

    private static final String DB_NAME = "aihackathon";
    private static final String OUTBOX_COLLECTION = "product_catalog_outbox";
    private static final int OUTBOX_BATCH_SIZE = 100;
    private static final long OUTBOX_TICK_INTERVAL_MS = 500;

    static void main() throws Exception {
        Injector injector = Guice.createInjector(new ProductCatalogModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "product_catalog_commands";
        channel.queueDeclare(queueName, true, false, false, null);

        ObjectMapper objectMapper = new ObjectMapper();
        MessageDispatcherImpl dispatcher = new MessageDispatcherImpl(objectMapper);

        dispatcher.registerHandler("CreateProductCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(CreateProductHandler.class),
                        CreateProductCommand.class));
        dispatcher.registerHandler("UpdateProductDetailsCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(UpdateProductDetailsHandler.class),
                        UpdateProductDetailsCommand.class));
        dispatcher.registerHandler("ChangeProductPriceCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(ChangeProductPriceHandler.class),
                        ChangeProductPriceCommand.class));
        dispatcher.registerHandler("ActivateProductCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(ActivateProductHandler.class),
                        ActivateProductCommand.class));
        dispatcher.registerHandler("DeactivateProductCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(DeactivateProductHandler.class),
                        DeactivateProductCommand.class));
        dispatcher.registerHandler("GetProductSnapshotsCommand",
                new CommandHandlerDispatcherAdapter<>(injector.getInstance(GetProductSnapshotsHandler.class),
                        GetProductSnapshotsCommand.class));

        OutboxDispatcher outboxDispatcher = new OutboxDispatcher(
                injector.getInstance(OutboxStore.class),
                injector.getInstance(MessageBus.class),
                OUTBOX_BATCH_SIZE);
        ScheduledExecutorService outboxScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "product-catalog-outbox-dispatcher");
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

        System.out.println("ProductCatalog CommandHandler waiting for messages (Dispatcher Pattern) on " + queueName);

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

    static class ProductCatalogModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MongoClient.class).toProvider(MongoClientProvider.class);
            bind(com.google.inject.Key.get(new TypeLiteral<Repository<Product, ProductId>>() {
            })).to(MongoProductRepository.class);
            bind(MessageBus.class).to(ProductCatalogMessageBus.class);
        }

        @Provides
        @Singleton
        OutboxStore outboxStore(MongoClient mongoClient) {
            return new MongoOutboxStore(mongoClient, DB_NAME, OUTBOX_COLLECTION);
        }
    }
}
