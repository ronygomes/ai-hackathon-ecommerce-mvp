package me.ronygomes.ecommerce.cart.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartItemTest {

    private static ProductId pid() {
        return new ProductId(UUID.randomUUID());
    }

    @Test
    void construct_storesProductAndQuantity() {
        ProductId p = pid();
        CartItem item = new CartItem(p, new Quantity(3));

        assertThat(item.getProductId()).isEqualTo(p);
        assertThat(item.getQuantity().value()).isEqualTo(3);
    }

    @Test
    void nullProductId_throws() {
        assertThatThrownBy(() -> new CartItem(null, new Quantity(1)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addQuantity_addsToCurrentValue() {
        CartItem item = new CartItem(pid(), new Quantity(2));

        item.addQuantity(new Quantity(3));

        assertThat(item.getQuantity().value()).isEqualTo(5);
    }

    @Test
    void updateQuantity_replacesValue() {
        CartItem item = new CartItem(pid(), new Quantity(2));

        item.updateQuantity(new Quantity(10));

        assertThat(item.getQuantity().value()).isEqualTo(10);
    }

    @Test
    void increaseQuantity_addsDelta() {
        CartItem item = new CartItem(pid(), new Quantity(5));

        item.increaseQuantity(4);

        assertThat(item.getQuantity().value()).isEqualTo(9);
    }
}
