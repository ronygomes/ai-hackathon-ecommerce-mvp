package com.ecommerce.cart.processes.commandhandler;

import com.ecommerce.core.application.ICommandHandler;
import com.ecommerce.core.infrastructure.MongoClientProvider;
import com.ecommerce.core.infrastructure.IRepository;
import com.ecommerce.core.messaging.IMessageBus;
import com.ecommerce.cart.application.*;
import com.ecommerce.cart.domain.ShoppingCart;
import com.ecommerce.cart.domain.CartId;
import com.ecommerce.cart.infrastructure.MongoCartRepository;
import com.ecommerce.cart.infrastructure.ICartRepository;
import com.ecommerce.cart.infrastructure.CartMessageBus;
import tools.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
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
        ICommandHandler<CreateCartCommand, Void> createHandler = injector.getInstance(CreateCartHandler.class);
        ICommandHandler<AddCartItemCommand, Void> addHandler = injector.getInstance(AddCartItemHandler.class);
        ICommandHandler<UpdateCartItemQtyCommand, Void> updateHandler = injector
                .getInstance(UpdateCartItemQtyHandler.class);
        ICommandHandler<RemoveCartItemCommand, Void> removeHandler = injector.getInstance(RemoveCartItemHandler.class);
        ICommandHandler<ClearCartCommand, Void> clearHandler = injector.getInstance(ClearCartHandler.class);

        System.out.println("CartCommandHandler waiting for messages on " + queueName);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            AMQP.BasicProperties props = delivery.getProperties();
            Map<String, Object> headers = props.getHeaders();
            String messageType = headers != null && headers.containsKey("X-Message-Type")
                    ? headers.get("X-Message-Type").toString()
                    : "";

            try {
                if ("CreateCartCommand".equals(messageType)) {
                    CreateCartCommand command = objectMapper.readValue(message, CreateCartCommand.class);
                    createHandler.handle(command).get();
                } else if ("AddCartItemCommand".equals(messageType)) {
                    AddCartItemCommand command = objectMapper.readValue(message, AddCartItemCommand.class);
                    addHandler.handle(command).get();
                } else if ("UpdateCartItemQtyCommand".equals(messageType)) {
                    UpdateCartItemQtyCommand command = objectMapper.readValue(message, UpdateCartItemQtyCommand.class);
                    updateHandler.handle(command).get();
                } else if ("RemoveCartItemCommand".equals(messageType)) {
                    RemoveCartItemCommand command = objectMapper.readValue(message, RemoveCartItemCommand.class);
                    removeHandler.handle(command).get();
                } else if ("ClearCartCommand".equals(messageType)) {
                    ClearCartCommand command = objectMapper.readValue(message, ClearCartCommand.class);
                    clearHandler.handle(command).get();
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

    static class CartModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(com.mongodb.client.MongoClient.class)
                    .toProvider(com.ecommerce.core.infrastructure.MongoClientProvider.class);
            bind(ICartRepository.class).to(MongoCartRepository.class);
            bind(com.google.inject.Key.get(new TypeLiteral<IRepository<ShoppingCart, CartId>>() {
            })).to(MongoCartRepository.class);
            bind(IMessageBus.class).to(CartMessageBus.class);
        }
    }
}
