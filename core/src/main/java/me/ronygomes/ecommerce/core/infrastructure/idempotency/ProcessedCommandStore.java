package me.ronygomes.ecommerce.core.infrastructure.idempotency;

public interface ProcessedCommandStore {

    boolean wasProcessed(String commandId);

    void markProcessed(String commandId, String metadata);
}
