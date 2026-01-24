package com.ecommerce.core.messaging;

import com.ecommerce.core.application.ICommandHandler;
import java.util.concurrent.CompletableFuture;

public class CommandHandlerDispatcherAdapter<TCommand, TResult> implements IMessageHandler<TCommand> {
    private final ICommandHandler<TCommand, TResult> handler;
    private final Class<TCommand> commandType;

    public CommandHandlerDispatcherAdapter(ICommandHandler<TCommand, TResult> handler, Class<TCommand> commandType) {
        this.handler = handler;
        this.commandType = commandType;
    }

    @Override
    public CompletableFuture<Void> handle(TCommand command) {
        return handler.handle(command).thenApply(r -> null);
    }

    @Override
    public Class<TCommand> getMessageType() {
        return commandType;
    }
}
