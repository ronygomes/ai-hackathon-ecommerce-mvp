package me.ronygomes.ecommerce.checkout.saga.message.event;

import me.ronygomes.ecommerce.core.domain.DomainEvent;

import java.util.List;
import java.util.UUID;

public record StockBatchValidationFailed(
        List<RejectedItem> rejected,
        String eventId,
        long timestamp) implements DomainEvent {

    public record RejectedItem(UUID productId, int requestedQty, int availableQty, String reason) {
    }

    public StockBatchValidationFailed(List<RejectedItem> rejected) {
        this(rejected, UUID.randomUUID().toString(), System.currentTimeMillis());
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }
}
