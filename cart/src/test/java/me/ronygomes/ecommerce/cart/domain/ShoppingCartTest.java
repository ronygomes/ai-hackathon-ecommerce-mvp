package me.ronygomes.ecommerce.cart.domain;

import me.rongyomes.ecommerce.checkout.saga.message.event.CartCleared;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingCartTest {

    private static ShoppingCart newCart() {
        return ShoppingCart.create(CartId.generate(), new GuestToken("guest-1"));
    }

    private static ProductId pid() {
        return new ProductId(UUID.randomUUID());
    }

    @Test
    void create_storesIdAndGuestTokenAndStartsEmpty() {
        CartId id = CartId.generate();
        ShoppingCart cart = ShoppingCart.create(id, new GuestToken("g1"));

        assertThat(cart.getId()).isEqualTo(id);
        assertThat(cart.getGuestToken().value()).isEqualTo("g1");
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getUncommittedEvents()).isEmpty();
    }

    @Test
    void addItem_newProduct_appendsCartItem() {
        ShoppingCart cart = newCart();
        ProductId p = pid();

        cart.addItem(p, new Quantity(2));

        assertThat(cart.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(p);
            assertThat(item.getQuantity().value()).isEqualTo(2);
        });
    }

    @Test
    void addItem_existingProduct_increasesQuantityInPlace() {
        ShoppingCart cart = newCart();
        ProductId p = pid();
        cart.addItem(p, new Quantity(2));

        cart.addItem(p, new Quantity(3));

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity().value()).isEqualTo(5));
    }

    @Test
    void removeItem_dropsMatchingProduct() {
        ShoppingCart cart = newCart();
        ProductId keep = pid();
        ProductId drop = pid();
        cart.addItem(keep, new Quantity(1));
        cart.addItem(drop, new Quantity(1));

        cart.removeItem(drop);

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getProductId()).isEqualTo(keep));
    }

    @Test
    void removeItem_unknownProduct_isNoOp() {
        ShoppingCart cart = newCart();
        cart.addItem(pid(), new Quantity(1));

        cart.removeItem(pid());

        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void updateQuantity_replacesItemForKnownProduct() {
        ShoppingCart cart = newCart();
        ProductId p = pid();
        cart.addItem(p, new Quantity(2));

        cart.updateQuantity(p, new Quantity(7));

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity().value()).isEqualTo(7));
    }

    @Test
    void changeQuantity_mutatesItemInPlace() {
        ShoppingCart cart = newCart();
        ProductId p = pid();
        cart.addItem(p, new Quantity(2));

        cart.changeQuantity(p, new Quantity(11));

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity().value()).isEqualTo(11));
    }

    @Test
    void clear_emptiesItemsAndEmitsCartCleared() {
        ShoppingCart cart = newCart();
        cart.addItem(pid(), new Quantity(2));
        cart.addItem(pid(), new Quantity(3));

        cart.clear();

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CartCleared.class,
                        e -> assertThat(e.guestToken()).isEqualTo(cart.getId().value().toString()));
    }

    @Test
    void addItem_doesNotEmitDomainEvent_currentBuggyBehavior() {
        // Spec asks for CartItemAdded to be emitted. Current impl mutates silently.
        // This test pins today's behavior so a future fix is intentional.
        ShoppingCart cart = newCart();

        cart.addItem(pid(), new Quantity(1));

        assertThat(cart.getUncommittedEvents()).isEmpty();
    }
}
