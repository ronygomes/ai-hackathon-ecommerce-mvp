package me.ronygomes.ecommerce.productcatalog.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    private static Product newProduct() {
        return Product.create(
                ProductId.generate(),
                new Sku("SKU-123"),
                new ProductName("Test Product"),
                new Price(99.99),
                new ProductDescription("Description"));
    }

    @Test
    void create_withValidData_initialisesFieldsAndEmitsProductCreated() {
        Product product = newProduct();

        assertThat(product.getId()).isNotNull();
        assertThat(product.getSku().value()).isEqualTo("SKU-123");
        assertThat(product.getName().value()).isEqualTo("Test Product");
        assertThat(product.getPrice().value()).isEqualTo(99.99);
        assertThat(product.getDescription().value()).isEqualTo("Description");
        assertThat(product.isActive()).isFalse();
        assertThat(product.getUncommittedEvents()).singleElement().isInstanceOf(ProductCreated.class);
    }

    @Test
    void updateDetails_setsFieldsAndEmitsProductDetailsUpdated() {
        Product product = newProduct();
        product.clearUncommittedEvents();

        product.updateDetails(new ProductName("New Name"), new ProductDescription("New Desc"));

        assertThat(product.getName().value()).isEqualTo("New Name");
        assertThat(product.getDescription().value()).isEqualTo("New Desc");
        assertThat(product.getUncommittedEvents()).singleElement().isInstanceOf(ProductDetailsUpdated.class);
    }

    @Test
    void changePrice_updatesPriceAndEmitsProductPriceChangedWithOldAndNew() {
        Product product = newProduct();
        product.clearUncommittedEvents();

        product.changePrice(new Price(150.00));

        assertThat(product.getPrice().value()).isEqualTo(150.00);
        assertThat(product.getUncommittedEvents()).singleElement()
                .isInstanceOfSatisfying(ProductPriceChanged.class, e -> {
                    assertThat(e.oldPrice().value()).isEqualTo(99.99);
                    assertThat(e.newPrice().value()).isEqualTo(150.00);
                });
    }

    @Test
    void activate_flipsIsActiveTrueAndEmitsProductActivated() {
        Product product = newProduct();
        product.clearUncommittedEvents();

        product.activate();

        assertThat(product.isActive()).isTrue();
        assertThat(product.getUncommittedEvents()).singleElement().isInstanceOf(ProductActivated.class);
    }

    @Test
    void deactivate_flipsIsActiveFalseAndEmitsProductDeactivated() {
        Product product = newProduct();
        product.activate();
        product.clearUncommittedEvents();

        product.deactivate();

        assertThat(product.isActive()).isFalse();
        assertThat(product.getUncommittedEvents()).singleElement().isInstanceOf(ProductDeactivated.class);
    }
}
