package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.command.MarkCheckoutCompletedCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockDeductedForOrder;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;

import java.util.concurrent.CompletableFuture;

public class StockDeductedForOrderHandler implements MessageHandler<StockDeductedForOrder> {

    private final CommandBus orderBus;
    private final SagaStateStore store;

    public StockDeductedForOrderHandler(CommandBus orderBus, SagaStateStore store) {
        this.orderBus = orderBus;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(StockDeductedForOrder event) {
        SagaState state = store.findByOrderId(event.orderId()).orElse(null);
        if (state != null) {
            state.deductedItemsCount++;
            store.save(state);
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
