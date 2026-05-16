package me.ronygomes.ecommerce.core.infrastructure;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import me.ronygomes.ecommerce.core.application.Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RabbitMQCommandBusTest {

    private record SampleCommand(String name, int qty) implements Command<Void> {
    }

    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;
    private RabbitMQCommandBus bus;

    @BeforeEach
    void setUp() throws Exception {
        factory = mock(ConnectionFactory.class);
        connection = mock(Connection.class);
        channel = mock(Channel.class);
        when(factory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);

        bus = new RabbitMQCommandBus("test_queue", factory);
    }

    @Test
    void send_declaresDurableQueueAndPublishesSerializedCommand() throws Exception {
        SampleCommand command = new SampleCommand("widget", 3);

        bus.send(command).get();

        verify(channel).queueDeclare(eq("test_queue"), eq(true), eq(false), eq(false), any());

        ArgumentCaptor<AMQP.BasicProperties> propsCaptor = ArgumentCaptor.forClass(AMQP.BasicProperties.class);
        ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(channel).basicPublish(eq(""), eq("test_queue"), propsCaptor.capture(), bodyCaptor.capture());

        assertThat(propsCaptor.getValue().getHeaders())
                .containsEntry("X-Message-Type", "SampleCommand");
        assertThat(new String(bodyCaptor.getValue()))
                .contains("\"name\":\"widget\"")
                .contains("\"qty\":3");
    }

    @Test
    void send_closesConnectionAfterPublish() throws Exception {
        bus.send(new SampleCommand("a", 1)).get();

        verify(channel).close();
        verify(connection).close();
    }

    @Test
    void send_whenChannelThrows_completesExceptionally() throws Exception {
        when(channel.queueDeclare(any(), anyBoolean(), anyBoolean(), anyBoolean(), any()))
                .thenThrow(new IOException("rabbit down"));

        assertThatThrownBy(() -> bus.send(new SampleCommand("a", 1)).get())
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Failed to send command to RabbitMQ");
    }
}
