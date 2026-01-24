package com.ecommerce.productcatalog.processes.commandhandler;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.productcatalog.application.CreateProductCommand;
import com.ecommerce.productcatalog.application.CreateProductHandler;
import com.ecommerce.productcatalog.domain.Product;
import com.ecommerce.productcatalog.infrastructure.MongoProductRepository;
import com.ecommerce.productcatalog.infrastructure.ProductCatalogMessageBus;
import tools.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.*;
import java.util.UUID;

public class CommandHandlerProcess {
    public static void main(String[] args) throws Exception {
        Injector injector = Guice.createInjector(new ProductCatalogModule());

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        String queueName = "product_catalog_commands";
        channel.queueDeclare(queueName, true, false, false, null);

        ObjectMapper objectMapper = new ObjectMapper();
        ICommandHandler<CreateProductCommand, UUID> createHandler = injector.getInstance(CreateProductHandler.class);

        System.out.println("CommandHandler waiting for messages on " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            try {
                CreateProductCommand command = objectMapper.readValue(message, CreateProductCommand.class);
                createHandler.handle(command).get(); // Wait for completion in this consumer thread
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                e.printStackTrace();
                // channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true); //
                // Basic retry
            }
        };

        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }

    static class ProductCatalogModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(MongoClient.class).toProvider(MongoClientProvider.class);
            bind(new TypeLiteral<IRepository<Product, UUID>>() {
            }).to(MongoProductRepository.class);
            bind(IMessageBus.class).to(ProductCatalogMessageBus.class);
        }
    }
}
