package me.ronygomes.ecommerce.core.infrastructure;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import me.ronygomes.ecommerce.core.domain.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitMQMessageBusTest {

    public record EventA(String getEventId, long getTimestamp, String payload) implements DomainEvent {
    }

    public record EventB(String getEventId, long getTimestamp, int amount) implements DomainEvent {
    }

    private Channel channel;
    private Connection connection;
    private RabbitMQMessageBus bus;

    @BeforeEach
    void setUp() throws Exception {
        ConnectionFactory factory = mock(ConnectionFactory.class);
        connection = mock(Connection.class);
        channel = mock(Channel.class);
        when(factory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);

        bus = new RabbitMQMessageBus("test_exchange", factory);
    }

    @Test
    void publish_declaresFanoutExchangeAndPublishesEachEventWithTypeHeader() throws Exception {
        bus.publish(List.of(new EventA("a", 1L, "hi"), new EventB("b", 2L, 42))).get();

        verify(channel).exchangeDeclare(eq("test_exchange"), eq("fanout"), eq(true));

        ArgumentCaptor<AMQP.BasicProperties> propsCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(channel, times(2))
                .basicPublish(eq("test_exchange"), eq(""), propsCaptor.capture(), bodyCaptor.capture());

        assertThat(propsCaptor.getAllValues())
                .extracting(p -> p.getHeaders().get("X-Message-Type"))
                .containsExactly("EventA", "EventB");
        assertThat(new String(bodyCaptor.getAllValues().get(0))).contains("\"payload\":\"hi\"");
        assertThat(new String(bodyCaptor.getAllValues().get(1))).contains("\"amount\":42");
    }

    @Test
    void publish_emptyList_declaresExchangeButPublishesNothing() throws Exception {
        bus.publish(List.of()).get();

        verify(channel).exchangeDeclare(eq("test_exchange"), eq("fanout"), eq(true));
        verify(channel, times(0)).basicPublish(any(), any(), any(), any());
    }

    @Test
    void publish_whenChannelThrows_completesExceptionally() throws Exception {
        when(channel.exchangeDeclare(any(String.class), any(String.class), anyBoolean()))
                .thenThrow(new IOException("rabbit down"));

        assertThatThrownBy(() -> bus.publish(List.of(new EventA("a", 1L, "x"))).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to publish events to RabbitMQ");
    }
}
