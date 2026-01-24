package com.ecommerce.productcatalog.processes.commandhandler;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.productcatalog.application.*;
import com.ecommerce.productcatalog.domain.Product;
import com.ecommerce.productcatalog.domain.ProductId;
import com.ecommerce.productcatalog.infrastructure.MongoProductRepository;
import com.ecommerce.productcatalog.infrastructure.ProductCatalogMessageBus;
import tools.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.*;

import java.util.Map;

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
        ICommandHandler<CreateProductCommand, ProductId> createHandler = injector
                .getInstance(CreateProductHandler.class);
        ICommandHandler<UpdateProductDetailsCommand, Void> updateHandler = injector
                .getInstance(UpdateProductDetailsHandler.class);
        ICommandHandler<ChangeProductPriceCommand, Void> priceHandler = injector
                .getInstance(ChangeProductPriceHandler.class);
        ICommandHandler<ActivateProductCommand, Void> activateHandler = injector
                .getInstance(ActivateProductHandler.class);
        ICommandHandler<DeactivateProductCommand, Void> deactivateHandler = injector
                .getInstance(DeactivateProductHandler.class);

        System.out.println("CommandHandler waiting for messages on " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("CreateProductCommand".equals(messageType)) {
                    CreateProductCommand command = objectMapper.readValue(message, CreateProductCommand.class);
                    createHandler.handle(command).get();
                } else if ("UpdateProductDetailsCommand".equals(messageType)) {
                    UpdateProductDetailsCommand command = objectMapper.readValue(message,
                            UpdateProductDetailsCommand.class);
                    updateHandler.handle(command).get();
                } else if ("ChangeProductPriceCommand".equals(messageType)) {
                    ChangeProductPriceCommand command = objectMapper.readValue(message,
                            ChangeProductPriceCommand.class);
                    priceHandler.handle(command).get();
                } else if ("ActivateProductCommand".equals(messageType)) {
                    ActivateProductCommand command = objectMapper.readValue(message, ActivateProductCommand.class);
                    activateHandler.handle(command).get();
                } else if ("DeactivateProductCommand".equals(messageType)) {
                    DeactivateProductCommand command = objectMapper.readValue(message, DeactivateProductCommand.class);
                    deactivateHandler.handle(command).get();
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

    static class ProductCatalogModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(com.mongodb.client.MongoClient.class)
                    .toProvider(com.ecommerce.core.infrastructure.MongoClientProvider.class);
            bind(com.google.inject.Key.get(new TypeLiteral<IRepository<Product, ProductId>>() {
            })).to(MongoProductRepository.class);
            bind(IMessageBus.class).to(ProductCatalogMessageBus.class);
        }
    }
}
