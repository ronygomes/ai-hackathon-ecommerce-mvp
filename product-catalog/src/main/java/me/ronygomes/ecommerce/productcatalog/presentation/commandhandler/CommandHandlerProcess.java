package me.ronygomes.ecommerce.productcatalog.presentation.commandhandler;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.*;
import me.rongyomes.ecommerce.checkout.saga.message.command.GetProductSnapshotsCommand;
import me.ronygomes.ecommerce.core.infrastructure.MongoClientProvider;
import me.ronygomes.ecommerce.core.infrastructure.Repository;
import me.ronygomes.ecommerce.core.messaging.CommandHandlerDispatcherAdapter;
import me.ronygomes.ecommerce.core.messaging.MessageBus;
import me.ronygomes.ecommerce.core.messaging.MessageDispatcherImpl;
import me.ronygomes.ecommerce.productcatalog.application.*;
import me.ronygomes.ecommerce.productcatalog.domain.Product;
import me.ronygomes.ecommerce.productcatalog.domain.ProductId;
import me.ronygomes.ecommerce.productcatalog.infrastructure.MongoProductRepository;
import me.ronygomes.ecommerce.productcatalog.infrastructure.ProductCatalogMessageBus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class CommandHandlerProcess {
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

        // Register handlers using Adapter
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

        System.out.println("ProductCatalog CommandHandler waiting for messages (Dispatcher Pattern) on " + queueName);

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

    static class ProductCatalogModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MongoClient.class).toProvider(MongoClientProvider.class);
            bind(com.google.inject.Key.get(new TypeLiteral<Repository<Product, ProductId>>() {
            })).to(MongoProductRepository.class);
            bind(MessageBus.class).to(ProductCatalogMessageBus.class);
        }
    }
}
