package me.ronygomes.ecommerce.cart.domain;

import me.ronygomes.ecommerce.checkout.saga.message.event.CartCleared;
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
    void create_storesIdAndGuestTokenAndEmitsCartCreated() {
        CartId id = CartId.generate();
        ShoppingCart cart = ShoppingCart.create(id, new GuestToken("g1"));

        assertThat(cart.getId()).isEqualTo(id);
        assertThat(cart.getGuestToken().value()).isEqualTo("g1");
        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CartCreated.class, e -> {
                    assertThat(e.cartId()).isEqualTo(id.value());
                    assertThat(e.guestToken()).isEqualTo("g1");
                });
    }

    @Test
    void addItem_newProduct_appendsCartItemAndEmitsCartItemAdded() {
        ShoppingCart cart = newCart();
        cart.clearUncommittedEvents(); // discard CartCreated from setup
        ProductId p = pid();

        cart.addItem(p, new Quantity(2));

        assertThat(cart.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo(p);
            assertThat(item.getQuantity().value()).isEqualTo(2);
        });
        assertThat(cart.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CartItemAdded.class, e -> {
                    assertThat(e.cartId()).isEqualTo(cart.getId().value());
                    assertThat(e.productId()).isEqualTo(p.value());
                    assertThat(e.qty()).isEqualTo(2);
                });
    }

    @Test
    void addItem_existingProduct_increasesQuantityAndEmitsCartItemQuantityUpdated() {
        ShoppingCart cart = newCart();
        ProductId p = pid();
        cart.addItem(p, new Quantity(2));
        cart.clearUncommittedEvents();

        cart.addItem(p, new Quantity(3));

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity().value()).isEqualTo(5));
        assertThat(cart.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CartItemQuantityUpdated.class, e -> {
                    assertThat(e.productId()).isEqualTo(p.value());
                    assertThat(e.oldQty()).isEqualTo(2);
                    assertThat(e.newQty()).isEqualTo(5);
                });
    }

    @Test
    void removeItem_dropsMatchingProductAndEmitsCartItemRemoved() {
        ShoppingCart cart = newCart();
        ProductId keep = pid();
        ProductId drop = pid();
        cart.addItem(keep, new Quantity(1));
        cart.addItem(drop, new Quantity(1));
        cart.clearUncommittedEvents();

        cart.removeItem(drop);

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getProductId()).isEqualTo(keep));
        assertThat(cart.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CartItemRemoved.class, e -> {
                    assertThat(e.cartId()).isEqualTo(cart.getId().value());
                    assertThat(e.productId()).isEqualTo(drop.value());
                });
    }

    @Test
    void removeItem_unknownProduct_isNoOpAndEmitsNothing() {
        ShoppingCart cart = newCart();
        cart.addItem(pid(), new Quantity(1));
        cart.clearUncommittedEvents();

        cart.removeItem(pid());

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getUncommittedEvents()).isEmpty();
    }

    @Test
    void updateQuantity_replacesItemAndEmitsCartItemQuantityUpdated() {
        ShoppingCart cart = newCart();
        ProductId p = pid();
        cart.addItem(p, new Quantity(2));
        cart.clearUncommittedEvents();

        cart.updateQuantity(p, new Quantity(7));

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity().value()).isEqualTo(7));
        assertThat(cart.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CartItemQuantityUpdated.class, e -> {
                    assertThat(e.productId()).isEqualTo(p.value());
                    assertThat(e.oldQty()).isEqualTo(2);
                    assertThat(e.newQty()).isEqualTo(7);
                });
    }

    @Test
    void changeQuantity_mutatesItemInPlaceAndEmitsCartItemQuantityUpdated() {
        ShoppingCart cart = newCart();
        ProductId p = pid();
        cart.addItem(p, new Quantity(2));
        cart.clearUncommittedEvents();

        cart.changeQuantity(p, new Quantity(11));

        assertThat(cart.getItems()).singleElement()
                .satisfies(item -> assertThat(item.getQuantity().value()).isEqualTo(11));
        assertThat(cart.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CartItemQuantityUpdated.class, e -> {
                    assertThat(e.productId()).isEqualTo(p.value());
                    assertThat(e.oldQty()).isEqualTo(2);
                    assertThat(e.newQty()).isEqualTo(11);
                });
    }

    @Test
    void clear_emptiesItemsAndEmitsCartClearedWithCorrelationId() {
        ShoppingCart cart = newCart();
        cart.addItem(pid(), new Quantity(2));
        cart.addItem(pid(), new Quantity(3));
        cart.clearUncommittedEvents();
        UUID correlationId = UUID.randomUUID();

        cart.clear(correlationId);

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(CartCleared.class, e -> {
                    assertThat(e.guestToken()).isEqualTo(cart.getId().value().toString());
                    assertThat(e.correlationId()).isEqualTo(correlationId);
                });
    }
}
