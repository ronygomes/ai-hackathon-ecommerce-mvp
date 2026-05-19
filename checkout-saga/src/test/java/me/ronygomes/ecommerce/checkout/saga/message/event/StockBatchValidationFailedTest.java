package me.ronygomes.ecommerce.checkout.saga.message.event;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockBatchValidationFailedTest {

    @Test
    void convenienceConstructor_populatesEventIdAndTimestamp() {
        UUID pid = UUID.randomUUID();
        StockBatchValidationFailed.RejectedItem rejected =
                new StockBatchValidationFailed.RejectedItem(pid, 5, 1, "Insufficient stock");

        UUID correlationId = UUID.randomUUID();
        StockBatchValidationFailed event = new StockBatchValidationFailed(List.of(rejected), correlationId, "test-cause");

        assertThat(event.rejected()).singleElement().isSameAs(rejected);
        assertThat(event.correlationId()).isEqualTo(correlationId);
        assertThat(event.eventId()).isNotBlank();
        assertThat(event.getEventId()).isEqualTo(event.eventId());
        assertThat(event.timestamp()).isPositive();
        assertThat(event.getTimestamp()).isEqualTo(event.timestamp());
    }
}
