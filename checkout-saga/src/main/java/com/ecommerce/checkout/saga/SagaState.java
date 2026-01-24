package com.ecommerce.checkout.saga;

import com.ecommerce.checkout.saga.messages.events.CartSnapshotProvided;
import com.ecommerce.checkout.saga.messages.events.ProductSnapshotsProvided;
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

    public SagaState(UUID orderId, String guestToken, String idempotencyKey) {
        this.orderId = orderId;
        this.guestToken = guestToken;
        this.idempotencyKey = idempotencyKey;
    }
}
