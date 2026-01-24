package com.ecommerce.core.application;

import java.util.concurrent.CompletableFuture;

public interface ICommandHandler<TCommand extends ICommand<TResponse>, TResponse> {
    CompletableFuture<TResponse> handle(TCommand command);
}
