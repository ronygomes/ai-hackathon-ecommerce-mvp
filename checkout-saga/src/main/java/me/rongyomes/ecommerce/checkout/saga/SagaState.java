package me.rongyomes.ecommerce.checkout.saga;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import me.rongyomes.ecommerce.checkout.saga.message.event.CartSnapshotProvided;
import me.rongyomes.ecommerce.checkout.saga.message.event.ProductSnapshotsProvided;

import java.util.List;
import java.util.UUID;

public class SagaState {
    public final UUID orderId;
    public final String guestToken;
    public final String idempotencyKey;

    public boolean stockValidated = false;
    public int totalItemsToDeduct = 0;
    public int deductedItemsCount = 0;

    public List<CartSnapshotProvided.CartItemSnapshot> cartItems;
    public List<ProductSnapshotsProvided.ProductSnapshot> productSnapshots;

    @JsonCreator
    public SagaState(@JsonProperty("orderId") UUID orderId,
                     @JsonProperty("guestToken") String guestToken,
                     @JsonProperty("idempotencyKey") String idempotencyKey) {
        this.orderId = orderId;
        this.guestToken = guestToken;
        this.idempotencyKey = idempotencyKey;
    }
}
