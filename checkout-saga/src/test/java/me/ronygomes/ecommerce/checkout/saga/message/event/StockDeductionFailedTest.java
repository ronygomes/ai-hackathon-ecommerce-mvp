package me.ronygomes.ecommerce.checkout.saga.message.event;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockDeductionFailedTest {

    @Test
    void convenienceConstructor_populatesEventIdAndTimestamp() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        StockDeductionFailed event = new StockDeductionFailed(orderId, productId, 5, 2, "Insufficient stock");

        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.productId()).isEqualTo(productId);
        assertThat(event.requestedQty()).isEqualTo(5);
        assertThat(event.availableQty()).isEqualTo(2);
        assertThat(event.reason()).isEqualTo("Insufficient stock");
        assertThat(event.getEventId()).isNotBlank();
        assertThat(event.getTimestamp()).isPositive();
    }
}
