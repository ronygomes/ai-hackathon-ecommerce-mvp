package me.ronygomes.ecommerce.core.infrastructure.outbox;

import me.ronygomes.ecommerce.core.messaging.MessageBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class OutboxDispatcher {

    private final OutboxStore store;
    private final MessageBus messageBus;
    private final int batchSize;

    public OutboxDispatcher(OutboxStore store, MessageBus messageBus, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0");
        }
        this.store = store;
        this.messageBus = messageBus;
        this.batchSize = batchSize;
    }

    public int tick() {
        List<OutboxEntry> pending = store.findPending(batchSize);
        if (pending.isEmpty()) return 0;

        List<MessageBus.RawMessage> messages = new ArrayList<>(pending.size());
        List<String> ids = new ArrayList<>(pending.size());
        for (OutboxEntry entry : pending) {
            messages.add(new MessageBus.RawMessage(entry.eventType(), entry.payload().getBytes()));
            ids.add(entry.id());
        }

        try {
            messageBus.publishRaw(messages).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Outbox dispatch interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Outbox dispatch failed", e.getCause());
        }

        store.markPublished(ids);
        return pending.size();
    }
}
