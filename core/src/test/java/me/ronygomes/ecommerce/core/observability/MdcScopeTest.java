package me.ronygomes.ecommerce.core.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MdcScopeTest {

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void with_singleEntry_putsAndRemovesOnClose() {
        try (var ignored = MdcScope.with("commandId", "cmd-1")) {
            assertThat(MDC.get("commandId")).isEqualTo("cmd-1");
        }
        assertThat(MDC.get("commandId")).isNull();
    }

    @Test
    void with_multipleEntries_putsAndRemovesAllOnClose() {
        try (var ignored = MdcScope.with(Map.of("commandId", "cmd-1", "correlationId", "corr-1"))) {
            assertThat(MDC.get("commandId")).isEqualTo("cmd-1");
            assertThat(MDC.get("correlationId")).isEqualTo("corr-1");
        }
        assertThat(MDC.get("commandId")).isNull();
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void with_nullValueInSingleKey_doesNotThrowAndDoesNotPut() {
        try (var ignored = MdcScope.with("commandId", null)) {
            assertThat(MDC.get("commandId")).isNull();
        }
        assertThat(MDC.get("commandId")).isNull();
    }

    @Test
    void with_nullValueInMap_skipsThatEntryButProcessesOthers() {
        Map<String, String> entries = new HashMap<>();
        entries.put("commandId", "cmd-1");
        entries.put("correlationId", null);
        try (var ignored = MdcScope.with(entries)) {
            assertThat(MDC.get("commandId")).isEqualTo("cmd-1");
            assertThat(MDC.get("correlationId")).isNull();
        }
        assertThat(MDC.get("commandId")).isNull();
    }

    @Test
    void close_removesEvenIfMDCExternallyClearedFirst() {
        try (var ignored = MdcScope.with("commandId", "cmd-1")) {
            MDC.clear();
            // close on the unwinding try should not throw
        }
        assertThat(MDC.get("commandId")).isNull();
    }
}
