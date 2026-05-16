package me.ronygomes.ecommerce.core.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BaseAggregateTest {

    private static final class TestAggregate extends BaseAggregate<UUID> {
        TestAggregate(UUID id) {
            this.id = id;
        }

        void emit(DomainEvent event) {
            addEvent(event);
        }
    }

    private record TestEvent(String getEventId, long getTimestamp) implements DomainEvent {
    }

    @Test
    void newAggregate_hasNoUncommittedEventsAndStartsAtVersionZero() {
        TestAggregate aggregate = new TestAggregate(UUID.randomUUID());

        assertThat(aggregate.getUncommittedEvents()).isEmpty();
        assertThat(aggregate.getVersion()).isZero();
    }

    @Test
    void addEvent_appendsToUncommittedEvents() {
        TestAggregate aggregate = new TestAggregate(UUID.randomUUID());
        DomainEvent first = new TestEvent("e1", 1L);
        DomainEvent second = new TestEvent("e2", 2L);

        aggregate.emit(first);
        aggregate.emit(second);

        assertThat(aggregate.getUncommittedEvents()).containsExactly(first, second);
    }

    @Test
    void clearUncommittedEvents_emptiesTheList() {
        TestAggregate aggregate = new TestAggregate(UUID.randomUUID());
        aggregate.emit(new TestEvent("e1", 1L));

        aggregate.clearUncommittedEvents();

        assertThat(aggregate.getUncommittedEvents()).isEmpty();
    }

    @Test
    void getUncommittedEvents_returnsUnmodifiableView() {
        TestAggregate aggregate = new TestAggregate(UUID.randomUUID());
        aggregate.emit(new TestEvent("e1", 1L));

        assertThatThrownBy(() -> aggregate.getUncommittedEvents().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getId_returnsConfiguredId() {
        UUID id = UUID.randomUUID();
        TestAggregate aggregate = new TestAggregate(id);

        assertThat(aggregate.getId()).isEqualTo(id);
    }
}
