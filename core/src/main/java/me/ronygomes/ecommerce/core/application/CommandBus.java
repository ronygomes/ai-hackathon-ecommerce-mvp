package me.ronygomes.ecommerce.core.application;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CommandBus {
    CompletableFuture<UUID> send(Command<?> command);
}
