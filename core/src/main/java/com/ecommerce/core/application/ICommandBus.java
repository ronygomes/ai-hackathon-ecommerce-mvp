package com.ecommerce.core.application;

import java.util.concurrent.CompletableFuture;

public interface ICommandBus {
    <TResponse> CompletableFuture<TResponse> send(ICommand<TResponse> command);
}
