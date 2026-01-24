package com.ecommerce.checkout.saga.handlers;

import com.ecommerce.core.application.ICommandBus;
import com.ecommerce.core.messaging.IMessageHandler;
import com.ecommerce.checkout.saga.messages.commands.MarkCheckoutCompletedCommand;
import com.ecommerce.checkout.saga.messages.events.StockDeductedForOrder;
import com.ecommerce.checkout.saga.SagaState;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SagaStockDeductedHandler implements IMessageHandler<StockDeductedForOrder> {
    private final Map<UUID, SagaState> activeSagas;
    private final ICommandBus orderBus;

    public SagaStockDeductedHandler(Map<UUID, SagaState> activeSagas, ICommandBus orderBus) {
        this.activeSagas = activeSagas;
        this.orderBus = orderBus;
    }

    @Override
    public CompletableFuture<Void> handle(StockDeductedForOrder event) {
        SagaState state = activeSagas.get(event.orderId());
        if (state != null) {
            state.deductedItemsCount++;
            if (state.deductedItemsCount >= state.totalItemsToDeduct) {
                orderBus.send(new MarkCheckoutCompletedCommand(state.orderId));
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Class<StockDeductedForOrder> getMessageType() {
        return StockDeductedForOrder.class;
    }
}
