package me.ronygomes.ecommerce.core.application;

import java.util.concurrent.CompletableFuture;

public interface CommandBus {
    <TResponse> CompletableFuture<TResponse> send(Command<TResponse> command);
}
