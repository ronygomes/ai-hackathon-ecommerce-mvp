package com.ecommerce.productcatalog.domain;

import com.ecommerce.core.domain.BaseAggregate;

public class Product extends BaseAggregate<ProductId> {
    private Sku sku;
    private ProductName name;
    private Price price;
    private ProductDescription description;
    private boolean isActive;

    public Product() {
        // Required for Jackson
    }

    private Product(ProductId id, Sku sku, ProductName name, Price price, ProductDescription description) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.description = description;
        this.isActive = false;
    }

    public static Product create(Sku sku, ProductName name, Price price, ProductDescription description) {
        ProductId id = ProductId.generate();
        Product product = new Product(id, sku, name, price, description);
        product.addEvent(new ProductCreated(id, sku, name, price, description));
        return product;
    }

    public void updateDetails(ProductName name, ProductDescription description) {
        this.name = name;
        this.description = description;
        addEvent(new ProductDetailsUpdated(this.id, name, description));
    }

    public void changePrice(Price newPrice) {
        Price oldPrice = this.price;
        this.price = newPrice;
        addEvent(new ProductPriceChanged(this.id, oldPrice, newPrice));
    }

    public void activate() {
        this.isActive = true;
        addEvent(new ProductActivated(this.id));
    }

    public void deactivate() {
        this.isActive = false;
        addEvent(new ProductDeactivated(this.id));
    }

    // Getters for persistence
    public Sku getSku() {
        return sku;
    }

    public ProductName getName() {
        return name;
    }

    public Price getPrice() {
        return price;
    }

    public ProductDescription getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }
}
