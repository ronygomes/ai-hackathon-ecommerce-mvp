package me.ronygomes.ecommerce.core.infrastructure;

import java.util.Objects;

/**
 * Externalized runtime configuration. Read once at process boot via {@link #fromEnv()}
 * and bound into Guice (or passed explicitly) so every wiring site downstream pulls
 * host / database settings from a single source.
 *
 * <p>Scope is intentionally narrow — only env-deployment values that varied between
 * docker-compose and other environments. Domain-stable names (queue names, exchange
 * names, collection names) stay hardcoded at their wiring site.
 */
public record AppConfig(String mongoUri, String mongoDbName, String rabbitHost) {

    public AppConfig {
        Objects.requireNonNull(mongoUri, "mongoUri");
        Objects.requireNonNull(mongoDbName, "mongoDbName");
        Objects.requireNonNull(rabbitHost, "rabbitHost");
    }

    public static AppConfig fromEnv() {
        return new AppConfig(
                env("MONGO_URI", "mongodb://admin:admin@localhost:27017"),
                env("MONGO_DB", "ecommerce_mvp"),
                env("RABBITMQ_HOST", "localhost"));
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? defaultValue : v;
    }
}
