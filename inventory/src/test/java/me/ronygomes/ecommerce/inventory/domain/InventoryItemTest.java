package me.ronygomes.ecommerce.inventory.domain;

import me.rongyomes.ecommerce.checkout.saga.message.event.StockDeductedForOrder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryItemTest {

    @Test
    void create_setsIdAndQuantityAndEmitsStockItemCreated() {
        ProductId pid = ProductId.generate();

        InventoryItem item = InventoryItem.create(pid, new Quantity(7));

        assertThat(item.getId()).isEqualTo(pid);
        assertThat(item.getQuantity().value()).isEqualTo(7);
        assertThat(item.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(StockItemCreated.class, e -> {
                    assertThat(e.productId()).isEqualTo(pid.value());
                    assertThat(e.initialQty()).isEqualTo(7);
                });
    }

    @Test
    void setStock_updatesQuantityAndEmitsStockSetWithOldAndNew() {
        InventoryItem item = InventoryItem.create(ProductId.generate(), new Quantity(10));
        item.clearUncommittedEvents();

        item.setStock(new Quantity(3), new AdjustmentReason("recount"));

        assertThat(item.getQuantity().value()).isEqualTo(3);
        assertThat(item.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(StockSet.class, e -> {
                    assertThat(e.oldQty()).isEqualTo(10);
                    assertThat(e.newQty()).isEqualTo(3);
                    assertThat(e.reason()).isEqualTo("recount");
                    assertThat(e.changedBy()).isEqualTo("admin");
                });
    }

    @Test
    void deductStock_whenAvailable_subtractsAndEmitsDeductedEvent() {
        ProductId pid = ProductId.generate();
        InventoryItem item = InventoryItem.create(pid, new Quantity(10));
        item.clearUncommittedEvents();
        UUID orderId = UUID.randomUUID();

        item.deductStock(new Quantity(4), orderId.toString());

        assertThat(item.getQuantity().value()).isEqualTo(6);
        assertThat(item.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(StockDeductedForOrder.class, e -> {
                    assertThat(e.orderId()).isEqualTo(orderId);
                    assertThat(e.productId()).isEqualTo(pid.value());
                    assertThat(e.newQty()).isEqualTo(6);
                });
    }

    @Test
    void deductStock_whenInsufficient_throwsAndDoesNotChangeState() {
        InventoryItem item = InventoryItem.create(ProductId.generate(), new Quantity(2));
        item.clearUncommittedEvents();

        assertThatThrownBy(() -> item.deductStock(new Quantity(5), UUID.randomUUID().toString()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");
        assertThat(item.getQuantity().value()).isEqualTo(2);
        assertThat(item.getUncommittedEvents()).isEmpty();
    }

    @Test
    void isAvailable_reportsBasedOnCurrentQuantity() {
        InventoryItem item = InventoryItem.create(ProductId.generate(), new Quantity(5));

        assertThat(item.isAvailable(new Quantity(5))).isTrue();
        assertThat(item.isAvailable(new Quantity(3))).isTrue();
        assertThat(item.isAvailable(new Quantity(6))).isFalse();
    }
}
