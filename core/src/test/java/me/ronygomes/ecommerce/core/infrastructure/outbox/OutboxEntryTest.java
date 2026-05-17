package me.ronygomes.ecommerce.core.infrastructure.outbox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEntryTest {

    @Test
    void pending_populatesIdAndCreatedAtAndLeavesPublishedAtNull() {
        OutboxEntry entry = OutboxEntry.pending("agg-1", "ProductCreated", "{\"x\":1}");

        assertThat(entry.id()).isNotBlank();
        assertThat(entry.aggregateId()).isEqualTo("agg-1");
        assertThat(entry.eventType()).isEqualTo("ProductCreated");
        assertThat(entry.payload()).isEqualTo("{\"x\":1}");
        assertThat(entry.createdAt()).isPositive();
        assertThat(entry.publishedAt()).isNull();
        assertThat(entry.isPending()).isTrue();
    }

    @Test
    void isPending_isFalseWhenPublishedAtSet() {
        OutboxEntry entry = new OutboxEntry("x", "agg", "T", "{}", 1L, 2L);

        assertThat(entry.isPending()).isFalse();
    }
}
