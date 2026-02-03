package me.ronygomes.ecommerce.core.application;

import java.util.concurrent.CompletableFuture;

public interface CommandHandler<TCommand extends Command<TResponse>, TResponse> {
    CompletableFuture<TResponse> handle(TCommand command);
}
