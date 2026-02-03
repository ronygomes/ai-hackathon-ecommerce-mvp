package me.ronygomes.ecommerce.productcatalog.domain;

public record ProductDescription(String value) {
    public ProductDescription {
        // Description can be null or empty in some contexts, but we'll allow it for
        // now.
    }
}
