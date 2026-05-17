package me.ronygomes.ecommerce.core.messaging;

import me.ronygomes.ecommerce.core.application.Command;
import me.ronygomes.ecommerce.core.application.CommandHandler;
import me.ronygomes.ecommerce.core.infrastructure.idempotency.ProcessedCommandStore;

import java.util.concurrent.CompletableFuture;

public class CommandHandlerDispatcherAdapter<TCommand extends Command<TResult>, TResult>
        implements MessageHandler<TCommand> {

    private final CommandHandler<TCommand, TResult> handler;
    private final Class<TCommand> commandType;
    private final ProcessedCommandStore processedCommandStore;

    public CommandHandlerDispatcherAdapter(CommandHandler<TCommand, TResult> handler, Class<TCommand> commandType) {
        this(handler, commandType, null);
    }

    public CommandHandlerDispatcherAdapter(CommandHandler<TCommand, TResult> handler,
                                           Class<TCommand> commandType,
                                           ProcessedCommandStore processedCommandStore) {
        this.handler = handler;
        this.commandType = commandType;
        this.processedCommandStore = processedCommandStore;
    }

    @Override
    public CompletableFuture<Void> handle(TCommand command) {
        return handler.handle(command).thenApply(r -> null);
    }

    @Override
    public CompletableFuture<Void> handle(TCommand command, MessageMetadata metadata) {
        if (processedCommandStore == null || metadata == null || metadata.commandId() == null) {
            return handle(command);
        }

        String commandId = metadata.commandId();
        if (processedCommandStore.wasProcessed(commandId)) {
            return CompletableFuture.completedFuture(null);
        }

        return handler.handle(command).thenApply(r -> {
            processedCommandStore.markProcessed(commandId, commandType.getSimpleName());
            return null;
        });
    }

    @Override
    public Class<TCommand> getMessageType() {
        return commandType;
    }
}
