package me.ronygomes.ecommerce.checkout.saga.handler;

import me.ronygomes.ecommerce.checkout.saga.SagaState;
import me.ronygomes.ecommerce.checkout.saga.SagaStateStore;
import me.ronygomes.ecommerce.checkout.saga.message.command.CancelOrderCommand;
import me.ronygomes.ecommerce.checkout.saga.message.event.StockDeductionFailed;
import me.ronygomes.ecommerce.core.application.CommandBus;
import me.ronygomes.ecommerce.core.messaging.MessageHandler;
import me.ronygomes.ecommerce.core.observability.MdcScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class StockDeductionFailedHandler implements MessageHandler<StockDeductionFailed> {

    private static final Logger log = LoggerFactory.getLogger(StockDeductionFailedHandler.class);

    private final CommandBus orderBus;
    private final SagaStateStore store;

    public StockDeductionFailedHandler(CommandBus orderBus, SagaStateStore store) {
        this.orderBus = orderBus;
        this.store = store;
    }

    @Override
    public CompletableFuture<Void> handle(StockDeductionFailed event) {
        try (var ignored = MdcScope.with("orderId", event.orderId().toString())) {
            SagaState state = store.findByOrderId(event.orderId()).orElse(null);
            if (state != null) {
                String reason = "stock deduction failed for product " + event.productId() + " (" + event.reason() + ")";
                log.warn("Saga ABORTED for order {}: {}", state.orderId, reason);
                orderBus.send(new CancelOrderCommand(state.orderId, reason));
                store.remove(state.orderId);
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    @Override
    public Class<StockDeductionFailed> getMessageType() {
        return StockDeductionFailed.class;
    }
}
