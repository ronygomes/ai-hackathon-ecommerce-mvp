package me.ronygomes.ecommerce.core.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppConfigTest {

    @Test
    void constructor_storesAllFields() {
        AppConfig config = new AppConfig("mongodb://m:27017", "shop", "rabbit");

        assertThat(config.mongoUri()).isEqualTo("mongodb://m:27017");
        assertThat(config.mongoDbName()).isEqualTo("shop");
        assertThat(config.rabbitHost()).isEqualTo("rabbit");
    }

    @Test
    void constructor_nullMongoUri_throws() {
        assertThatThrownBy(() -> new AppConfig(null, "shop", "rabbit"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullDbName_throws() {
        assertThatThrownBy(() -> new AppConfig("uri", null, "rabbit"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullRabbitHost_throws() {
        assertThatThrownBy(() -> new AppConfig("uri", "shop", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fromEnv_withoutEnvVarsSet_usesDockerComposeDefaults() {
        // This test asserts the DEFAULT values when the env vars aren't set.
        // CI/dev typically does not set these. If a developer has these env vars
        // set locally they'll see a benign test failure and know to unset them.
        AppConfig config = AppConfig.fromEnv();

        if (System.getenv("MONGO_URI") == null || System.getenv("MONGO_URI").isBlank()) {
            assertThat(config.mongoUri()).isEqualTo("mongodb://admin:admin@localhost:27017");
        }
        if (System.getenv("MONGO_DB") == null || System.getenv("MONGO_DB").isBlank()) {
            assertThat(config.mongoDbName()).isEqualTo("ecommerce_mvp");
        }
        if (System.getenv("RABBIT_HOST") == null || System.getenv("RABBIT_HOST").isBlank()) {
            assertThat(config.rabbitHost()).isEqualTo("localhost");
        }
    }
}
